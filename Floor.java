import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Floor {

  private Model floor;

  public Floor(GL3 gl, Light light, Camera camera, Texture diffuse) {
    String name = "floor";
    Mesh mesh = new Mesh(gl, TwoTriangles.vertices.clone(), TwoTriangles.indices.clone());
    Mat4 modelMatrix = getM1();
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    Material material = new Material(new Vec3(0.1f, 0.5f, 0.91f), new Vec3(0.1f, 0.5f, 0.91f), new Vec3(0.3f, 0.3f, 0.3f), 4.0f);
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(null);
    material.setEmissionMap(null);
    Renderer renderer = new Renderer();
    floor = new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  public void render(GL3 gl) {
    floor.render(gl);
  }

  private Mat4 getM1() {
    float size = 50f;
    Mat4 modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(size,1f,size), modelMatrix);
    return modelMatrix;
  }
}
