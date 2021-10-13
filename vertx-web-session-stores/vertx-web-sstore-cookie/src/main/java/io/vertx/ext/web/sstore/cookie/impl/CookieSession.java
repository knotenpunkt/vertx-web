/*
 * Copyright 2018 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.web.sstore.cookie.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.web.sstore.AbstractSession;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;

import static io.vertx.ext.auth.impl.Codec.base64UrlDecode;
import static io.vertx.ext.auth.impl.Codec.base64UrlEncode;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class CookieSession extends AbstractSession {

  private final Mac mac;
  // track the original version
  private int oldVersion = 0;
  // track the original crc
  private int oldCrc = 0;

  public CookieSession(Mac mac, VertxContextPRNG prng, long timeout, int length) {
    super(prng, timeout, length);
    this.mac = mac;
  }

  public CookieSession(Mac mac, VertxContextPRNG prng) {
    super(prng);
    this.mac = mac;
  }

  @Override
  public String value() {

    Buffer payload = new JsonObject()
      .put("id", id())
      .put("timeout", timeout())
      .put("lastAccessed", lastAccessed())
      .put("version", version())
      .put("data", data())
      .toBuffer();

    String b64 = base64UrlEncode(payload.getBytes());
    String signature = base64UrlEncode(mac.doFinal(b64.getBytes(StandardCharsets.US_ASCII)));

    return b64 + "." + signature;
  }

  @Override
  public boolean isRegenerated() {
    if (!super.isRegenerated()) {
      // force a new checksum calculation
      return oldCrc != checksum();
    }

    return true;
  }


  protected CookieSession setValue(String payload) {

    if (payload == null) {
      throw new NullPointerException();
    }

    String[] tokens = payload.split("\\.");
    if (tokens.length != 2) {
      // no signature present, force a regeneration
      // by claiming this session as invalid
      return null;
    }

    String signature = base64UrlEncode(mac.doFinal(tokens[0].getBytes(StandardCharsets.US_ASCII)));

    if(!signature.equals(tokens[1])) {
      throw new RuntimeException("Session data was Tampered!");
    }

    // reconstruct the session
    JsonObject decoded = new JsonObject(Buffer.buffer(base64UrlDecode(tokens[0])));

    setId(decoded.getString("id"));
    setTimeout(decoded.getLong("timeout"));
    setLastAccessed(decoded.getLong("lastAccessed"));
    setVersion(decoded.getInteger("version"));
    setData(decoded.getJsonObject("data"));

    // defaults
    oldVersion = version();
    oldCrc = crc();

    return this;
  }

  int oldVersion() {
    return oldVersion;
  }
}
