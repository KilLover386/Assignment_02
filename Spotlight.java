import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Spotlight {

  private Model sphere;
  private Model bulb; // The glowing part
  private SGNode spotlightRoot;
  private SGTransformNode headTransform; 
  private Light light; 
  
  public Spotlight(GL3 gl, Light light, Camera camera, TextureLibrary textures) {
    this.light = light;
    // Metallic body
    this.sphere = makeSphere(gl, light, camera, textures.get("specular_container")); 
    // Glowing bulb
    this.bulb = makeBulb(gl, light, camera, textures.get("white1x1")); 
    makeSG();
  }

  public void update() {
    // 1. Animate
    double time = System.currentTimeMillis() / 1000.0;
    float angle = 45.0f * (float)Math.sin(time); 
    headTransform.setTransform(Mat4Transform.rotateAroundZ(angle)); 
    
    spotlightRoot.update(); 

    // 2. Update Light Source Position & Direction
    Mat4 worldT = headTransform.getWorldTransform();
    float[] matData = worldT.toFloatArrayForGLSL(); 
    
    // Position of the head
    Vec3 pos = new Vec3(matData[12], matData[13], matData[14]);
    
    // Direction calculation (Rotating around Z)
    float rad = (float)Math.toRadians(angle);
    float dirX = -(float)Math.sin(rad); 
    float dirY = -(float)Math.cos(rad);
    Vec3 direction = new Vec3(dirX, dirY, 0f); 
    direction.normalize();
    
    light.setPosition(pos);
    light.setDirection(direction);
  }

  public void render(GL3 gl) {
    spotlightRoot.draw(gl);
  }

  private void makeSG() {
    spotlightRoot = new SGNameNode("Spotlight Root");
    
    // 1. Root: Place in corner
    SGTransformNode transRoot = new SGTransformNode("Transform Root", Mat4Transform.translate(-8f, 0f, -8f));
    
    // 2. Base
    SGNameNode baseNode = new SGNameNode("Base");
    Mat4 baseM = Mat4Transform.translate(0, 1f, 0);
    baseM = Mat4.multiply(baseM, Mat4Transform.scale(2f, 2f, 2f));
    SGTransformNode baseTransform = new SGTransformNode("Base Scale", baseM);
    SGModelNode baseShape = new SGModelNode("Base Shape", sphere);

    // 3. Pole
    SGNameNode poleNode = new SGNameNode("Pole");
    Mat4 poleM = Mat4Transform.translate(0, 6f, 0); 
    poleM = Mat4.multiply(poleM, Mat4Transform.scale(0.5f, 10f, 0.5f)); 
    SGTransformNode poleTransform = new SGTransformNode("Pole Scale", poleM);
    SGModelNode poleShape = new SGModelNode("Pole Shape", sphere);

    // 4. Arm
    SGNameNode armNode = new SGNameNode("Arm");
    Mat4 armM = Mat4Transform.translate(2f, 10.5f, 0f); 
    armM = Mat4.multiply(armM, Mat4Transform.scale(4f, 0.5f, 0.5f)); 
    SGTransformNode armTransform = new SGTransformNode("Arm Scale", armM);
    SGModelNode armShape = new SGModelNode("Arm Shape", sphere);

    // 5. Head (Rotates)
    SGNameNode headNode = new SGNameNode("Head");
    headTransform = new SGTransformNode("Head Rotation", Mat4Transform.translate(4f, 10.5f, 0f));
    
    // Housing
    Mat4 headScaleM = Mat4Transform.scale(1.5f, 1.5f, 1.5f);
    SGTransformNode headScale = new SGTransformNode("Head Scale", headScaleM);
    SGModelNode headShape = new SGModelNode("Head Shape", sphere);

    // 6. Bulb (Emissive, at the bottom of the head)
    Mat4 bulbM = Mat4Transform.translate(0f, -0.6f, 0f); // Stick out slightly
    bulbM = Mat4.multiply(bulbM, Mat4Transform.scale(0.8f, 0.5f, 0.8f));
    SGTransformNode bulbTrans = new SGTransformNode("Bulb Trans", bulbM);
    SGModelNode bulbShape = new SGModelNode("Bulb Shape", bulb);

    // Tree Construction
    spotlightRoot.addChild(transRoot);
      transRoot.addChild(baseNode);
        baseNode.addChild(baseTransform);
          baseTransform.addChild(baseShape);
      transRoot.addChild(poleNode);
        poleNode.addChild(poleTransform);
          poleTransform.addChild(poleShape);
      transRoot.addChild(armNode);
        armNode.addChild(armTransform);
          armTransform.addChild(armShape);
      transRoot.addChild(headNode);
        headNode.addChild(headTransform);
          headTransform.addChild(headScale);
            headScale.addChild(headShape);
          headTransform.addChild(bulbTrans); // Add bulb to rotating head
            bulbTrans.addChild(bulbShape);
            
    spotlightRoot.update();
  }

  private Model makeSphere(GL3 gl, Light light, Camera camera, Texture diffuse) {
    String name = "spotlight_part";
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    Material material = new Material(new Vec3(0.3f, 0.3f, 0.3f), new Vec3(0.3f, 0.3f, 0.3f), new Vec3(0.8f, 0.8f, 0.8f), 32.0f);
    material.setDiffuseMap(diffuse);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  private Model makeBulb(GL3 gl, Light light, Camera camera, Texture diffuse) {
    String name = "spotlight_bulb";
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    // Emissive material (High emission value)
    Material material = new Material(new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), 32.0f);
    material.setDiffuseMap(diffuse);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }
}