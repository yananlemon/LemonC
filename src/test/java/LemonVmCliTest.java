import org.junit.Test;
import site.ilemon.compiler.LemonC;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LemonVmCliTest {

    @Test
    public void cliRunsVmBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--target", "vm"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertEquals("values=5,weights=3,total=8\n", normalize(out.toString("UTF-8")));
    }

    @Test
    public void cliDumpsVmBytecodeAndRunsProgram() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--target=vm", "--dump-vm-bytecode"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String output = normalize(out.toString("UTF-8"));
        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertTrue(output, output.contains("== LemonVM Bytecode =="));
        assertTrue(output, output.contains("NewArr"));
        assertTrue(output, output.endsWith("values=5,weights=3,total=8\n"));
    }

    @Test
    public void cliRejectsVmBytecodeDumpWithoutVmTarget() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--dump-vm-bytecode"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(1, exitCode);
        assertEquals("", out.toString("UTF-8"));
        assertTrue(err.toString("UTF-8").contains("--dump-vm-bytecode requires --target vm or --run-vm"));
    }

    @Test
    public void cliShowsFullVmPipeline() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--pipeline"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String output = normalize(out.toString("UTF-8"));
        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertTrue(output, output.contains("== TOKENS =="));
        assertTrue(output, output.contains("== AST =="));
        assertTrue(output, output.contains("== LemonIR =="));
        assertTrue(output, output.contains("== LemonVM Bytecode =="));
        assertTrue(output, output.contains("== LemonVM Output =="));
        assertTrue(output, output.endsWith("values=5,weights=3,total=8\n"));
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
