import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Walls {

  private Model wallFront, wallBack, wallLeft, wallRight;
  private Model wallRoof;
  private float size = 50f;
  private float height = 30f;

  public Walls(GL3 gl, Light light, Camera camera, TextureLibrary textures) {
    // assign textures per your request:
    // front -> "dog", back -> "cloud", left/right -> "matrix"
    wallFront = makeWall(gl, light, camera, textures.get("snow"));
    wallBack  = makeWall(gl, light, camera, textures.get("snow"));
    wallLeft  = makeWall(gl, light, camera, textures.get("snow"));
    wallRight = makeWall(gl, light, camera, textures.get("snow"));
    wallRoof = makeWall(gl, light, camera, textures.get("sky"));
  }

  private Model makeWall(GL3 gl, Light light, Camera camera, Texture diffuse) {
    String name = "wall";
    Mesh mesh = new Mesh(gl, TwoTriangles.vertices.clone(), TwoTriangles.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    Material material = new Material(new Vec3(0.1f, 0.5f, 0.91f),
                                     new Vec3(0.1f, 0.5f, 0.91f),
                                     new Vec3(0.3f, 0.3f, 0.3f), 4.0f);
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(null);
    material.setEmissionMap(null);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  public void render(GL3 gl) {
    wallFront.setModelMatrix(getWallFront());
    wallFront.render(gl);

    wallBack.setModelMatrix(getWallBack());
    wallBack.render(gl);

    wallLeft.setModelMatrix(getWallLeft());
    wallLeft.render(gl);

    wallRight.setModelMatrix(getWallRight());
    wallRight.render(gl);

    wallRoof.setModelMatrix(getWallRoof());
    wallRoof.render(gl);
  }

  private Mat4 getWallFront() {
    Mat4 modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(size, 1f, height), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.translate(0f, height/2f, -size/2f), modelMatrix);
    return modelMatrix;
  }

  private Mat4 getWallBack() {
    Mat4 modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(size, 1f, height), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundY(180), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(-90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.translate(0f, height/2f, size/2f), modelMatrix);
    return modelMatrix;
  }

  private Mat4 getWallLeft() {
    Mat4 modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(size, 1f, height), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundY(90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.translate(-size/2f, height/2f, 0f), modelMatrix);
    return modelMatrix;
  }

  private Mat4 getWallRight() {
    Mat4 modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(size, 1f, height), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundY(-90), modelMatrix);
    modelMatrix = Mat4.multiply(Mat4Transform.translate(size/2f, height/2f, 0f), modelMatrix);
    return modelMatrix;
  }

  private Mat4 getWallRoof() {
  Mat4 modelMatrix = new Mat4(1);
  modelMatrix = Mat4.multiply(Mat4Transform.scale(size, 1f, size), modelMatrix);
  modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(180), modelMatrix);
  modelMatrix = Mat4.multiply(Mat4Transform.translate(0f, height, 0f), modelMatrix);
  return modelMatrix;
  }
}