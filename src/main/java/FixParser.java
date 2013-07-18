package com.miriamlaurel.hardin;

import java.nio.ByteBuffer;
import static com.miriamlaurel.hardin.FixParser.ByteState.*;

public class FixParser {

    private ByteState state = GARBAGE;
    private ByteBuffer tagBuffer = ByteBuffer.allocate(10);
    private ByteBuffer valueBuffer = ByteBuffer.allocate(65535);

    public void onData(byte[] bytes) {
        for (byte b : bytes) {
            switch (state) {
                case GARBAGE:
                    if (isNumber(b)) {
                        tagBuffer.put(b);
                        state = TAGNUM;
                    }
                    break;
                case TAGNUM:
                    if (isNumber(b)) {
                        tagBuffer.put(b);
                    } else if (isEqualsSign(b)) {
                        tagBuffer.flip();
                        state = EQUALS;
                    } else {
                        fault();
                    }
                    break;
                case EQUALS:
                    if (isSplit(b)) {
                        fault();
                    } else {
                        valueBuffer.put(b);
                        state = VALUE;
                    }
                    break;
                case VALUE:
                    if (isEqualsSign(b)) {
                        valueBuffer.flip();
                        emitTag();
                        state = SPLIT;
                    } else {
                        valueBuffer.put(b);
                    }
                    break;
                case SPLIT:
                    if (isNumber(b)) {
                        tagBuffer.put(b);
                        state = TAGNUM;
                    } else {
                        fault();
                    }
                    break;
            }
        }
    }

    public void onTag(FixTag tag) {
    }

    private void emitTag() {
        int tagNum = tagBuffer.getInt();
        byte[] bytes = new byte[valueBuffer.remaining()];
        valueBuffer.get(bytes);
        String value = new String(bytes);
        FixTag tag = new FixTag(tagNum, value);
        tagBuffer.clear();
        valueBuffer.clear();
        onTag(tag);
    }

    private void fault() {
        tagBuffer.clear();
        valueBuffer.clear();
        state = GARBAGE;
        // todo reset all states
    }

    private boolean isNumber(byte b) {
        return b >= 0x30 && b <= 0x39;
    }

    private boolean isEqualsSign(byte b) {
        return b == 0x3D;
    }

    private boolean isSplit(byte b) {
        return b == 0x01;
    }

    enum ByteState {
        GARBAGE, TAGNUM, EQUALS, VALUE, SPLIT
    }

    class FixTag {
        public final int number;
        public final String value;

        FixTag(int number, String value) {
            this.number = number;
            this.value = value;
        }
    }
}
