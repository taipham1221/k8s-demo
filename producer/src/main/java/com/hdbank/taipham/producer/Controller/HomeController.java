package com.hdbank.taipham.producer.Controller;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.taipham.producer.Model.SaleRecord;
import com.hdbank.taipham.producer.producer.MessageProducer;

@RestController
public class HomeController {

    @Autowired
    MessageProducer messageProducer;
    @GetMapping("table1")
    public String login() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SaleRecord record;
        record = generateRecordFromTable1();
        String jsonRecord = objectMapper.writeValueAsString(record);

        messageProducer.sendMessage("SALE", jsonRecord);
        return jsonRecord;
        
    }
    @GetMapping("test")
    public String test() throws JsonProcessingException {
        return "oke";
    }
    private static SaleRecord generateRecordFromTable1() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -ThreadLocalRandom.current().nextInt(30));
        Timestamp timestamp = new Timestamp(cal.getTimeInMillis());

        return new SaleRecord(
                "user" + ThreadLocalRandom.current().nextInt(1, 100),
                "product" + ThreadLocalRandom.current().nextInt(1, 50),
                ThreadLocalRandom.current().nextInt(1, 10),
                ThreadLocalRandom.current().nextDouble(10, 100),
                timestamp,
                "Table1"
        );
    }
}
