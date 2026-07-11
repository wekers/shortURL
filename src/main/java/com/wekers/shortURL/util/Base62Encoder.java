package com.wekers.shortURL.util;

public class Base62Encoder {

    private static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private Base62Encoder() {
    }

    public static String encode(long number) {

        if (number == 0) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();

        while (number > 0) {

            builder.append(BASE62.charAt((int) (number % 62)));
            number /= 62;
        }

        return builder.reverse().toString();
    }
}
