/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.mongodb.sink.MongoSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.bson.BsonDocument;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

public class DataStreamJob {

	private static final Logger logger = LoggerFactory.getLogger(CRUD.class);
	public static void main(String[] args) throws Exception {

		String connectionString = "mongodb://root:12345678@mongo-service:27017";

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		KafkaSource<String> source = KafkaSource.<String>builder()
				.setBootstrapServers("kafka:29092")
				.setTopics("debezium.public.purchase")
				.setGroupId("secondary-group")
				.setStartingOffsets(OffsetsInitializer.earliest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();
		DataStream<String> kafkaStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source").filter(Objects::nonNull);
		kafkaStream.flatMap(new CRUD()).print();
		MongoSink<String> sink = MongoSink.<String>builder()
				.setUri(connectionString)
				.setDatabase("saless")
				.setCollection("purchase")
				.setBatchSize(1000)
				.setBatchIntervalMs(1000)
				.setMaxRetries(3)
				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
				.setSerializationSchema(
						(input, context) -> new InsertOneModel<>(BsonDocument.parse(input)))
				.build();

		kafkaStream.sinkTo(sink);
		env.execute("CDC to mongo");
	}



	static boolean preFlightChecks(MongoClient mongoClient) {
		Document pingCommand = new Document("ping", 1);
		Document response = mongoClient.getDatabase("admin").runCommand(pingCommand);
		System.out.println("=> Print result of the '{ping: 1}' command.");
		System.out.println(response.toJson(JsonWriterSettings.builder().indent(true).build()));
		return response.get("ok", Number.class).intValue() == 1;
	}
	public static class CRUD implements FlatMapFunction<String, String>{
		private static final String MONGO_CONNECTION_STRING = "mongodb://root:12345678@mongo-service:27017";
		private static final Logger logger = LoggerFactory.getLogger(CRUD.class);

		void create(UUID id, String username, String product, int quantity, float unitPrice, String currency, OffsetDateTime createdAt, OffsetDateTime updatedAt){
			CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
					MongoClientSettings.getDefaultCodecRegistry(),
					CodecRegistries.fromCodecs(new OffsetDateTimeCodec())
			);
			try (MongoClient mongoClient =MongoClients.create(
					MongoClientSettings.builder()
							.applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
							.codecRegistry(codecRegistry)
							.build())) {


				MongoDatabase sampleTrainingDB = mongoClient.getDatabase("sales");
				MongoCollection<Document> gradesCollection = sampleTrainingDB.getCollection("purchase");

				Random rand = new Random();
				Document purchase = new Document("_id", new ObjectId());
				purchase.append("id",id.toString())
						.append("username", username)
						.append("product", product)
						.append("quantity", quantity)
						.append("unitPrice", unitPrice)
						.append("currency", currency)
						.append("createdAt", createdAt)
						.append("updatedAt", updatedAt);
				gradesCollection.insertOne(purchase);
			}
		}

		void update(UUID id, String username, String product, int quantity, float unitPrice, String currency, OffsetDateTime createdAt, OffsetDateTime updatedAt){
			CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
					MongoClientSettings.getDefaultCodecRegistry(),
					CodecRegistries.fromCodecs(new OffsetDateTimeCodec())
			);
			try (MongoClient mongoClient =MongoClients.create(
					MongoClientSettings.builder()
							.applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
							.codecRegistry(codecRegistry)
							.build())) {


				MongoDatabase sampleTrainingDB = mongoClient.getDatabase("sales");
				MongoCollection<Document> gradesCollection = sampleTrainingDB.getCollection("purchase");

				Bson filter = eq("id",id.toString());
				Bson updateOperation = Updates.combine(
						set("username", username),
						set("product", product),
						set("quantity", quantity),
						set("unitPrice", unitPrice),
						set("currency", currency),
						set("createdAt", createdAt),
						set("updatedAt", updatedAt)
				);
				UpdateResult updateResult = gradesCollection.updateOne(filter, updateOperation);
			}
		}
		void delete(UUID id){
			CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
					MongoClientSettings.getDefaultCodecRegistry(),
					CodecRegistries.fromCodecs(new OffsetDateTimeCodec())
			);
			try (MongoClient mongoClient =MongoClients.create(
					MongoClientSettings.builder()
							.applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
							.codecRegistry(codecRegistry)
							.build())) {


				MongoDatabase sampleTrainingDB = mongoClient.getDatabase("sales");
				MongoCollection<Document> gradesCollection = sampleTrainingDB.getCollection("purchase");

				Bson filter = eq("id",id.toString());
				DeleteResult result = gradesCollection.deleteOne(filter);
			}
		}

		@Override
		public void flatMap(String json, Collector<String> collector) throws Exception {
			ObjectMapper mapper = new ObjectMapper();
			if(json!=null&&!json.isEmpty()){
				try{
					JsonNode root = mapper.readTree(json);

					JsonNode payloadNode = root.path("payload");
					if(!payloadNode.isMissingNode() && payloadNode.has("op")){
						String type = payloadNode.get("op").asText();
						if(Objects.equals(type, "c") || Objects.equals(type, "r")){
							JsonNode afterNode = root.path("payload").path("after");
							if (!afterNode.isMissingNode()) {
								create(
										UUID.fromString(afterNode.get("id").asText()),
										afterNode.get("username").asText(),
										afterNode.get("product").asText(),
										afterNode.get("quantity").asInt(),
										(float) afterNode.get("unit_price").asDouble(),
										afterNode.get("currency").asText(),
										OffsetDateTime.parse(afterNode.get("created_at").asText()),
										OffsetDateTime.parse(afterNode.get("updated_at").asText())
								);

							}

						}else if(Objects.equals(type, "u")){
							JsonNode afterNode = root.path("payload").path("after");
							update(
									UUID.fromString(afterNode.get("id").asText()),
									afterNode.get("username").asText(),
									afterNode.get("product").asText(),
									afterNode.get("quantity").asInt(),
									(float) afterNode.get("unit_price").asDouble(),
									afterNode.get("currency").asText(),
									OffsetDateTime.parse(afterNode.get("created_at").asText()),
									OffsetDateTime.parse(afterNode.get("updated_at").asText())
							);
						}else if(Objects.equals(type, "d")){
							JsonNode beforeNode = root.path("payload").path("before");
							delete(UUID.fromString(beforeNode.get("id").asText()));
						}
					}
				}catch (Exception e){
					logger.error("Error processing record: {}", json, e);
				}
			}


		}
	}
}
