package ru.yandex.practicum.kafka.telemetry.config;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer implements Serializer<org.apache.avro.specific.SpecificRecord> {
    @Override
    public byte[] serialize(String topic, org.apache.avro.specific.SpecificRecord data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<org.apache.avro.specific.SpecificRecord> writer = 
                new SpecificDatumWriter<>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();
            byte[] result = out.toByteArray();
            out.close();
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error serializing Avro message for topic " + topic, e);
        }
    }
}

