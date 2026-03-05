package omnivoxel.client.game.graphics.opengl.shader;

import omnivoxel.client.game.settings.ConstantGameSettings;
import org.lwjgl.opengl.GL30C;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.*;

public class ShaderProgramHandler {
    private final Map<String, ShaderProgram> shaderPrograms;

    public ShaderProgramHandler() {
        shaderPrograms = new HashMap<>();
    }

    public ShaderProgram getShaderProgram(String name) {
        return shaderPrograms.get(name);
    }

    public void addShaderProgram(String name, Map<String, Integer> shaderPaths) throws IOException {
        int programID = glCreateProgram();
        if (programID == 0) {
            throw new IOException("Error creating shader program.");
        }

        for (Map.Entry<String, Integer> entry : shaderPaths.entrySet()) {
            String path = entry.getKey();
            Integer type = entry.getValue();
            String contents = Files.readString(Path.of(ConstantGameSettings.DATA_LOCATION + path));

            int shaderID = glCreateShader(type);
            if (shaderID == 0) {
                throw new IOException("Error creating shader.");
            }

            glShaderSource(shaderID, contents);
            glCompileShader(shaderID);
            if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
                System.err.println("Error compiling shader: " + path);
                System.err.println(glGetShaderInfoLog(shaderID, 1024 * 8));
                throw new IOException("Shader compilation failed.");
            }

            glAttachShader(programID, shaderID);

            glDeleteShader(shaderID);
        }

        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Error linking shader program: " + glGetProgramInfoLog(programID, 1024));
            throw new IOException("Program linking failed.");
        }

        int vaoID = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(vaoID); // Bind a VAO before validating

        glValidateProgram(programID);

        if (glGetProgrami(programID, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println(
                    "Error validating shader program: " + glGetProgramInfoLog(programID, 1024));
            throw new IOException("Program validation failed.");
        }

        GL30C.glBindVertexArray(0); // Unbind after validation
        GL30C.glDeleteVertexArrays(vaoID); // Cleanup

        shaderPrograms.put(name, new ShaderProgram(programID));
    }

    public void unbind() {
        glUseProgram(0);
    }
}