import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class FObject {

  private Model fStemCube, fTopCube, fMiddleCube;
  private SGNode fShapeRoot;
  private float angle = 0;
  SGTransformNode rotateAroundY;

  public FObject(GL3 gl, Light light, Camera camera) {
    String fragmentPath = "assets/shaders/fs_standard_0t.txt";
    Material materialCube = MaterialConstants.blue.clone();
    fStemCube = makeCube(gl, light, camera);
    fTopCube = makeCube(gl, light, camera);
    fMiddleCube = makeCube(gl, light, camera);
    makeSG();
  }

  public void setAngle(float angle) {
    this.angle = angle;
    rotateAroundY.setTransform(Mat4Transform.rotateAroundY(angle));
    fShapeRoot.update();
  }

  private Model makeCube(GL3 gl, Light light, Camera camera) {
    String name = "cube";
    Mesh mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_0t.txt");
    Material material = MaterialConstants.blue.clone();
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  private void makeSG() {
    fShapeRoot = new SGNameNode("f structure");
    SGTransformNode translateFInPlane = new SGTransformNode("translate(0.2,0,1.7)", Mat4Transform.translate(0.2f,0f,2.7f));

    rotateAroundY = new SGTransformNode("rotate("+angle+",y)", Mat4Transform.rotateAroundY(angle));

    SGNameNode fStem = new SGNameNode("fStem");
    Mat4 m = Mat4Transform.scale(0.5f,3,1);
    m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    SGTransformNode fStemTransform = new SGTransformNode("some transforms", m);
    SGModelNode fStemShape = new SGModelNode("cubeLower", fStemCube);

    SGNameNode fMiddle = new SGNameNode("fMiddle");
    m = Mat4Transform.translate(0.5f,1f,0);
    m = Mat4.multiply(m, Mat4Transform.scale(1,0.5f,1));
    m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    SGTransformNode fMiddleTransform = new SGTransformNode("some transforms", m);
    SGModelNode fMiddleShape = new SGModelNode("cubeLower", fMiddleCube);

    SGNameNode fTop = new SGNameNode("fTop");
    m = Mat4Transform.translate(0.5f,2.5f,0);
    m = Mat4.multiply(m, Mat4Transform.scale(1.5f,0.5f,1));
    m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    SGTransformNode fTopTransform = new SGTransformNode("some transforms", m);
    SGModelNode fTopShape = new SGModelNode("cubeLower", fTopCube);

    fShapeRoot.addChild(translateFInPlane);
      translateFInPlane.addChild(rotateAroundY);  // added
      rotateAroundY.addChild(fStem);
        fStem.addChild(fStemTransform);
          fStemTransform.addChild(fStemShape);
        fStem.addChild(fMiddle);
          fMiddle.addChild(fMiddleTransform);
            fMiddleTransform.addChild(fMiddleShape);
        fStem.addChild(fTop);
          fTop.addChild(fTopTransform);
            fTopTransform.addChild(fTopShape);
    fShapeRoot.update();
  }

  public void render(GL3 gl) {
    fShapeRoot.draw(gl);
  }

}
