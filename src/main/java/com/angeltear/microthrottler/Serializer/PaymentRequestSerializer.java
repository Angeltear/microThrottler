package com.angeltear.microthrottler.Serializer;

import com.angeltear.microthrottler.Model.PaymentRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;

public class PaymentRequestSerializer {

    //Using Kryo according to Lettuce.io recommendation - https://github.com/lettuce-io/lettuce-core/wiki/Codecs
    private final Kryo kryo = new Kryo();

    public byte[] encode(PaymentRequest request) {
        kryo.register(PaymentRequest.class);
        Output output = new Output(4096, Integer.MAX_VALUE - 8);
        kryo.writeObject(output, request);
        output.close();
        return output.toBytes();
    }

    public PaymentRequest decode(byte[] inputValue) {
        kryo.register(PaymentRequest.class);
        ByteArrayInputStream bais = new ByteArrayInputStream(inputValue);
        Input input = new Input(bais);
        return kryo.readObject(input, PaymentRequest.class);
    }


}
