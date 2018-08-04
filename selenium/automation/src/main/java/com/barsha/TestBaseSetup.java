package com.barsha;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Predicate;

public class TestBaseSetup {
	protected static final String TESTCASE_FILE = "src/test/resources/data/TMS.csv";
	private static WebDriver driver;
	protected JavascriptExecutor jse;
	protected String defaultDriver;
	protected WebDriverWait wait;
	private String binaryLocation;
	static String driverPath = "lib";
	private static Properties config = new Properties();
	protected static Map<String, List<CsvRec>> recordsMap = new HashMap<>();
	protected Map<String, String> context = new HashMap<>();
	
	public static <T> T initPage(Class<T> clazz) {
		return PageFactory.initElements(driver, clazz);
	}

	public WebDriver getDriver(String driverName) {
		return driver;
	}

	public WebDriver getDriver() {
		return driver;
	}

	protected String getCurrentUrl() {
		return driver.getCurrentUrl();
	}

	private void setDriver() {
		if ("chrome".equals(defaultDriver)) {
			driver = initChromeDriver();	
		} else if("firefox".equals(defaultDriver)) {
			driver = initFirefoxDriver();
		}
		
	}

	private WebDriver initChromeDriver() {
		System.out.println("Launching google chrome with new profile..");
		System.setProperty("webdriver.chrome.driver", config.getProperty("chromeDriverLocation"));
		ChromeOptions options = new ChromeOptions();
		options.setBinary(binaryLocation);
		options.addArguments("--disable-extensions");
		options.setCapability("chrome.switches", Arrays.asList("--disable-extensions"));
		options.setExperimentalOption("useAutomationExtension", false);
		driver = new ChromeDriver(options);
		return driver;
	}
	
	private WebDriver initFirefoxDriver() {
		System.out.println("Launching Firefox browser.."+config.getProperty("appUrl"));
		System.setProperty("webdriver.gecko.driver", config.getProperty(defaultDriver+"DriverLocation"));
		FirefoxBinary binary = new FirefoxBinary(new File(binaryLocation));
		FirefoxOptions firefoxOptions = new FirefoxOptions();
		firefoxOptions.setBinary(binary);
		firefoxOptions.setProfile(new FirefoxProfile());
		driver = new FirefoxDriver(firefoxOptions);
		return driver;
	}


	//@Parameters({ "appURL" })
	public void initializeTestBaseSetup() throws IOException {
		config.load(new FileInputStream("src/main/resources/selenium.properties"));
		defaultDriver = config.getProperty("defaultDriver");
		binaryLocation = config.getProperty(defaultDriver+"BinaryLocation");
		setDriver();
		driver.manage().window().maximize();
		driver.navigate().to(config.getProperty("appUrl"));
		getDriver().manage().timeouts().implicitlyWait(Integer.parseInt(config.getProperty("waitTime", "100")), TimeUnit.SECONDS);
		wait = new WebDriverWait(getDriver(), Integer.parseInt(config.getProperty("waitTime", "100")));
		jse = (JavascriptExecutor) driver;
	}
	
	@BeforeClass
	public static void globalSetup() throws IOException {
		final CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader("Tescase Name", "﻿PAGE NAME", "CSS_CELL", "VALUE", "TYPE");
		 try (
		            Reader reader = Files.newBufferedReader(Paths.get(TESTCASE_FILE));
		            CSVParser csvParser = new CSVParser(reader, csvFileFormat);
		        ) {
		            for (CSVRecord csvRecord : csvParser.getRecords()) {
		            	if (csvRecord.getRecordNumber() > 1) {
		            		System.out.println(csvRecord.get(3));
		            		CsvRec rec = new CsvRec();
		            		String testcaseName = csvRecord.get(0);
		            		if (!recordsMap.containsKey(testcaseName)) {
		            			recordsMap.put(testcaseName, new ArrayList<>());
		            		}
			            	rec.setPageName(csvRecord.get(1));
			            	rec.setCssSelector(csvRecord.get(2));
			            	rec.setValue(csvRecord.get(3));
			            	if (csvRecord.get(4) != null && csvRecord.get(4).trim().length() > 0)
			            		rec.setType(ActionType.valueOf(csvRecord.get(4)));
			            	rec.setRegex(csvRecord.get(5));
			            	recordsMap.get(testcaseName).add(rec);	
		            	}
		            }
		        } catch (IOException e) {
					e.printStackTrace();
				}
	}
	
	protected void handleRecord(CsvRec rec) {
		System.out.println("Record: "+rec);
		try {
			switch (rec.getType()) {
			case CLICK:
			click(rec.getCssSelector(), rec.getValue());
			break;
			case SELECT:select(rec.getCssSelector(), rec.getValue());
				break;
			case SUBMIT:submit(rec.getCssSelector());
				break;
			case ENTRY:sendEntry(rec.getCssSelector(), rec.getValue());
				break;
			case SAVE:
				WebElement elem = getElem(rec.getCssSelector());
				String value = elem.getText().trim();
				if (rec.getRegex() != null && rec.getRegex().length() > 0) {
					Pattern p = Pattern.compile(rec.getRegex());
					Matcher m = p.matcher(value);
					while (m.find()) {
						value = m.group(1);	
					}
				}
				context.put(rec.getValue(), value);
				System.out.println(rec.getValue() + " : " +elem.getText().trim());
				break;
			case IFRAME: getDriver().switchTo().frame(getElem(rec.getCssSelector()));
				break;
			case UPLOAD:
				upload(rec.getCssSelector(), rec.getValue());
				break;
			case BACK: getDriver().switchTo().defaultContent();
				break;
			case WAIT:waitTimer(rec.getValue()==null ? 5000 : Integer.parseInt(rec.getValue()));
				break;
			case JS:
				executeJS(rec.getCssSelector(), rec.getValue());
				break;
			}
		} catch(Exception ex) {
			System.err.println("Erroneous Record: "+rec);
			ex.printStackTrace();
		}
	}

	private void executeJS(String cssSelector, String value) {
		jse.executeScript(value);
	}

	private void upload(String cssSel, String value) {
		WebElement elem = getElem(cssSel);
		elem.sendKeys(value);
	}

	protected WebElement getElem(String cssSel) {
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSel)));
		return getDriver().findElement(By.cssSelector(cssSel));
	}
	
	protected List<WebElement> getElems(String cssSel) {
		return getDriver().findElements(By.cssSelector(cssSel));
	}
	
	protected void sendEntry(String cssSel, String value) {
		WebElement elem = getElem(cssSel);
		elem.click();
		elem.clear();
		if (value.startsWith("${") && value.endsWith("}")) {
			String key = value.substring(2, value.length()-1);
			value = context.get(key);
		}
		
		elem.sendKeys(value);
	}
	
	protected void select(String cssSel, String value) {
		try {
			WebElement elem = getElem(cssSel);
			int index = Integer.parseInt(value);
			WebElement target = elem.findElement(By.cssSelector(":nth-child("+index+")"));
			//wait.until(ExpectedConditions.elementToBeClickable(target));
			wait.until(new Function<WebDriver, Object>() {
				@Override
				public Object apply(WebDriver t) {
					return new Select(elem).getOptions().size() > 1;
				}
			});
			new Select(elem).selectByIndex(index);
		} catch(NumberFormatException ex) {
			new Select(getElem(cssSel)).selectByVisibleText(value.trim());
		}
	}
	
	protected void submit(String cssSel){
		getElem(cssSel).submit();
	}
	
	protected void click(String cssSel, String value){
		WebElement elem = getElem(cssSel);
		wait.until(ExpectedConditions.visibilityOf(elem)); 
		wait.until(ExpectedConditions.elementToBeClickable(elem));
		
		if ("radio".equals(elem.getAttribute("type"))) {
			Actions actions = new Actions(getDriver());
			actions.moveToElement(elem).click().perform();
			return;
		} 
		
		
		if (value != null && value.length() > 0) {
			getElems(cssSel).stream()
				.filter(e -> e.getText().trim().equalsIgnoreCase(value))
				.findFirst().get()
				.click();
			return;
		} 
		
		elem.click();
	}
	
	protected void waitTimer(int val) {
		try {
			Thread.sleep(val);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}