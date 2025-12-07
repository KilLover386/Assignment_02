import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Cuboids {

  private Model cubeLower, cubeUpper;
  private SGNode cuboidsRoot;
  private float angle;
  private SGTransformNode rotateAroundY;

  public Cuboids(GL3 gl, Light light, Camera camera) {
    String fragmentPath = "assets/shaders/fs_standard_0t.txt";
    Material materialCube = MaterialConstants.blue.clone();
    cubeLower = makeCube(gl, light, camera, MaterialConstants.green.clone());
    cubeUpper = makeCube(gl, light, camera, MaterialConstants.red.clone());
    makeSG();
  }

  public void setAngle(float angle) {
    this.angle = angle;
    rotateAroundY.setTransform(Mat4Transform.rotateAroundY(angle));
    cuboidsRoot.update();
  }

  private Model makeCube(GL3 gl, Light light, Camera camera, Material material) {
    String name = "cube";
    Mesh mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_0t.txt");
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  private void makeSG() {
    cuboidsRoot = new SGNameNode("two-cuboid structure");

    SGTransformNode translateInPlane = new SGTransformNode("translate(1.7,0,1.3)",Mat4Transform.translate(2.7f, 0f, 2.3f));

    SGNameNode lowerCuboid = new SGNameNode("lower cuboid");
    rotateAroundY = new SGTransformNode("rotate("+angle+",y)", Mat4Transform.rotateAroundY(angle));
    Mat4 m = Mat4Transform.scale(1,1,1);
    m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    SGTransformNode lowerCubeTransform = new SGTransformNode("some transforms", m);
    SGModelNode lowerCubeShape = new SGModelNode("cubeLower", cubeLower);

    SGTransformNode translateToTop = new SGTransformNode("translate(0,1,0)",Mat4Transform.translate(0,1,0));

    SGNameNode upperCuboid = new SGNameNode("upper cuboid");
    m = Mat4Transform.scale(0.6f,3.5f,0.6f);
    m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    SGTransformNode upperCubeTransform = new SGTransformNode("some transforms", m);
    SGModelNode upperCubeShape = new SGModelNode("cubeUpper", cubeUpper);

    cuboidsRoot.addChild(translateInPlane);
      translateInPlane.addChild(lowerCuboid);
        lowerCuboid.addChild(rotateAroundY);
          rotateAroundY.addChild(lowerCubeTransform);  // added
          lowerCubeTransform.addChild(lowerCubeShape);
        lowerCuboid.addChild(translateToTop);
          translateToTop.addChild(upperCuboid);
            upperCuboid.addChild(upperCubeTransform);
              upperCubeTransform.addChild(upperCubeShape);
    cuboidsRoot.update(); 
  }

  public void render(GL3 gl) {
    cuboidsRoot.draw(gl);
  }
}
