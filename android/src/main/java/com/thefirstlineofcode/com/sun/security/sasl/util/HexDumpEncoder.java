package com.thefirstlineofcode.com.sun.security.sasl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HexDumpEncoder {
	public void encodeBuffer(ByteArrayInputStream in , ByteArrayOutputStream out) {
		Object impl = null;
		try {
			impl = Class.forName("sun.misc.HexDumpEncoder");
			
		} catch (Exception e1) {
			if (impl == null) {
				try {
					impl = Class.forName("sun.security.util.HexDumpEncoder");					
				} catch (Exception e2) {
					throw new RuntimeException("Can't intialize HexDumpEncoder implementation.", e2);
				}
			}
		}
		
		callEncodeBuffer(impl);
	}

	private void callEncodeBuffer(Object impl) {
		try {
			impl.getClass().getMethod("encodeBuffer",
					new Class<?>[] {ByteArrayInputStream.class, ByteArrayOutputStream.class});
		} catch (Exception e) {
			throw new RuntimeException("Can't call HexDumpEncoder.enableBuffer(....) method.", e);
		}
	}
}
