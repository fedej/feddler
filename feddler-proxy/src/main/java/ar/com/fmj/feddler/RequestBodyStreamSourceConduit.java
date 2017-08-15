package ar.com.fmj.feddler;

import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.ConduitReadableByteChannel;
import org.xnio.conduits.StreamSourceConduit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RequestBodyStreamSourceConduit extends AbstractStreamSourceConduit<StreamSourceConduit> {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    RequestBodyStreamSourceConduit(final StreamSourceConduit next) {
        super(next);
    }

    public long transferTo(final long position, final long count, final FileChannel target) throws IOException {
        return target.transferFrom(new ConduitReadableByteChannel(this), position, count);
    }

    public long transferTo(final long count, final ByteBuffer throughBuffer, final StreamSinkChannel target)
            throws IOException {
        return IoUtils.transfer(new ConduitReadableByteChannel(this), count, throughBuffer, target);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        int pos = dst.position();
        int res = super.read(dst);
        if (res > 0) {
            byte[] data = new byte[res];
            for (int i = 0; i < res; ++i) {
                data[i] = dst.get(i + pos);
            }
            out.write(data, pos, data.length);
        }
        return res;
    }

    @Override
    public long read(final ByteBuffer[] dsts, final int offs, final int len) throws IOException {
        for (int i = offs; i < len; ++i) {
            if (dsts[i].hasRemaining()) {
                return read(dsts[i]);
            }
        }
        return 0;
    }

    String getBody() {
        try {
            return out.toString("UTF-8");
        } catch (final UnsupportedEncodingException exc) {
            return null;
        }
    }
}
