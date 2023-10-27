package sd23;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

public class JobFunction {
    public JobFunction() {
    }

    public static byte[] execute(byte[] var0) throws JobFunctionException {
        try {
            ByteArrayOutputStream var1 = new ByteArrayOutputStream(var0.length);
            Random var2 = new Random();
            int var3 = var2.nextInt(100);
            if (var3 < 10) {
                throw new RuntimeException("Job computation failed due to runtime error.");
            } else {
                int var4 = var0.length > 0 ? var0.length : 1;
                int var5 = Math.max(1, Math.min((int)Math.ceil(Math.log((double)var4)), 10));
                System.out.println("Estimated time to compute: " + var5 + " seconds");
                Thread.sleep((long)var5 * 1000L);
                ByteArrayOutputStream var6 = var1;
                Throwable var7 = null;

                try {
                    GZIPOutputStream var8 = new GZIPOutputStream(var6);
                    Throwable var9 = null;

                    try {
                        var8.write(var0);
                    } catch (Throwable var34) {
                        var9 = var34;
                        throw var34;
                    } finally {
                        if (var8 != null) {
                            if (var9 != null) {
                                try {
                                    var8.close();
                                } catch (Throwable var33) {
                                    var9.addSuppressed(var33);
                                }
                            } else {
                                var8.close();
                            }
                        }

                    }
                } catch (Throwable var36) {
                    var7 = var36;
                    throw var36;
                } finally {
                    if (var1 != null) {
                        if (var7 != null) {
                            try {
                                var6.close();
                            } catch (Throwable var32) {
                                var7.addSuppressed(var32);
                            }
                        } else {
                            var1.close();
                        }
                    }

                }

                return var1.toByteArray();
            }
        } catch (Exception var38) {
            throw new JobFunctionException("Could not compute the job.", var38);
        }
    }
}