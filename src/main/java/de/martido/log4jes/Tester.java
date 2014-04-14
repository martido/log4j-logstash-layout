package de.martido.log4jes;

import org.apache.log4j.Logger;


public class Tester {

  private static final Logger logger = Logger.getLogger(Tester.class);

  public static void main(String[] args) {

    logger.info("This is a test");

    try {
      new String((byte[]) null);
    } catch (Exception ex) {
      logger.error("Oops!", ex);
    }
  }

}
