/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.internal.client;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Utility class with commonly used methods.
 *
 * @author Oliver Kuhl - Initial contribution
 *
 */
public class Util {

    public static DateTime convertZuluTimeStringToDate(String timeString) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.parseDateTime(timeString);
    }

    /**
     * Compares two strings, but returns null, if one of the strings is null.
     *
     * @param string1
     * @param string2
     * @return
     */
    public static Boolean equalsIfPresent(String string1, String string2) {
        if (string1 == null || string2 == null) {
            return null;
        } else if (string2.equals(string1)) {
            return true;
        } else {
            return false;
        }
    }
}
