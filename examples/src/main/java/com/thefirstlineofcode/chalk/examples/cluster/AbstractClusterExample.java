package com.thefirstlineofcode.chalk.examples.cluster;

import java.util.Collections;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.thefirstlineofcode.chalk.examples.AbstractExample;
import com.thefirstlineofcode.chalk.examples.Options;

public abstract class AbstractClusterExample extends AbstractExample {
	protected MongoClient dbClient = null;
	protected MongoDatabase database = null;
	
	protected void doInit(Options options) {
		dbClient = createDatabase(options);
		database = dbClient.getDatabase(options.dbName);
	}
	
	private MongoClient createDatabase(Options options) {
		MongoClient client = new MongoClient(new ServerAddress(options.dbHost, options.dbPort),
				Collections.singletonList(MongoCredential.createCredential(options.dbUser,
						options.dbName, options.dbPassword.toCharArray())));
		
		return client;
	}
	
	protected void cleanUsers() {
		database.getCollection("users").deleteMany(new Document());
	}
	
	@Override
	public void clean() {
		try {
			cleanData();
			cleanUsers();			
		} catch (Exception e) {
			throw new RuntimeException("Can't clean data and users correctly.", e);
		} finally {			
			dbClient.close();
		}
	}

	protected abstract void cleanData();

}
