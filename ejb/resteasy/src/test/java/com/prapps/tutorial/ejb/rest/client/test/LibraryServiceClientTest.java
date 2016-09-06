package com.prapps.tutorial.ejb.rest.client.test;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.prapps.tutorial.ejb.rest.model.Book;

public class LibraryServiceClientTest {
	private static final Logger LOG = Logger.getLogger(LibraryServiceClientTest.class.getName());
	private static final String url = "http://localhost:8080/restful-webservice/library/books";

	Book dummyBook;
	static {
		System.setProperty("java.util.logging.config.file", "src/test/resources/logging.properties");
	}
	
	@Before
	public void setUp() {
		dummyBook = new Book();
		dummyBook.setAuthor("Devdutt Patnaik");
		dummyBook.setIsbn("9788129137708");
		dummyBook.setTitle("My Gita");
		dummyBook.setPublishedDate(Calendar.getInstance());	
	}

	@Test
	public void testAddBook() {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Entity<Book> entity = Entity.entity(dummyBook, MediaType.APPLICATION_JSON);
		Response response = client.target(url).request().put(entity);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		LOG.fine("Server response : \n");
		
		Book retrieved = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE)
				.target(url).path("/{isbn}").resolveTemplate("isbn", dummyBook.getIsbn()).request().get().readEntity(Book.class);
		Assert.assertEquals(dummyBook, retrieved);
	}

	@Test
	public void testGetBookByIsbn() {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		WebTarget target = client.target(url).path("/{isbn}").resolveTemplate("isbn", dummyBook.getIsbn());
		Response response = target.request().get();
		Book book = response.readEntity(Book.class);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		LOG.fine("Server response : \n");
		LOG.fine("Title: " + book.getTitle() + "\tAuthor: " + book.getAuthor() + "\tISBN: " + book.getIsbn());
		Assert.assertEquals("My Gita", book.getTitle());
		Assert.assertEquals("Devdutt Patnaik", book.getAuthor());
	}
	
	@Test
	public void testGetBooks() {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		WebTarget target = client.target(url);
		Response response = target.request().get();
		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		GenericType<List<Book>> bookListType = new GenericType<List<Book>>() {};
		List<Book> books = response.readEntity(bookListType);
		Book book = books.get(0);
		Assert.assertEquals(dummyBook.getAuthor(), book.getAuthor());
		Assert.assertEquals(dummyBook.getIsbn(), book.getIsbn());
		Assert.assertEquals(dummyBook.getTitle(), book.getTitle());
	}

	@Test
	public void testGetBookByAuthorAndTitle() {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Map<String, Object> map = new HashMap<>();
		map.put("author", "Devdutt Patnaik");
		map.put("title", "My Gita");
		WebTarget target = client.target(url).path("/{author}/{title}").resolveTemplates(map);
		Response response = target.request().get();
		Book book = response.readEntity(Book.class);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		LOG.fine("Server response : \n");
		LOG.fine("Title: " + book.getTitle() + "\tAuthor: " + book.getAuthor() + "\tISBN: " + book.getIsbn());
		Assert.assertEquals("My Gita", book.getTitle());
		Assert.assertEquals("Devdutt Patnaik", book.getAuthor());
	}

	public enum AddHeadersFilter implements ClientRequestFilter {
		INSTANCE;

		private AddHeadersFilter() {
		}

		@Override
		public void filter(ClientRequestContext requestContext) throws IOException {
			/*
			 * String token = username + ":" + password; String base64Token =
			 * Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8))
			 * ;
			 */
			// requestContext.getHeaders().add("Authorization", "Basic " +
			// base64Token);
			// requestContext.getHeaders().add("X-Requested-With",
			// "XMLHttpRequest");
			requestContext.getHeaders().add("Accept", "application/xml");

		}
	}
}
