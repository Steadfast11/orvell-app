# Orvell R8 qoidalari. AndroidX/Compose/coroutines o'z qoidalarini AAR ichida olib keladi;
# bu yerda faqat ilovaning o'ziga xos ehtiyojlari.

# Crash xabarlarida qator raqamlari qolsin (mapping.txt bilan qayta tiklanadi), fayl nomi yashiriladi.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Sozlamalar JSON'ida enum nomlari saqlanadi (Units.METRIC.name -> "METRIC", keyin valueOf).
# Nom obfuskatsiya qilinsa, yangi versiya eski sozlamani o'qiy olmaydi.
-keepclassmembers enum uz.xasan.orvell.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Ilovada reflection, serializatsiya kutubxonasi yoki JNI yo'q; org.json Android tizimining qismi.
