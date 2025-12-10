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
    this.sphere = makeSphere(gl, light, camera, textures.get("specular_container")); 
    this.bulb = makeBulb(gl, light, camera, textures.get("white1x1")); 
    makeSG();
  }

  public void update() {
    // 1. Animate
    double time = System.currentTimeMillis() / 1000.0;
    float angle = 45.0f * (float)Math.sin(time); 
    
    Mat4 transform = Mat4Transform.translate(4f, 10.5f, 0f);
    transform = Mat4.multiply(transform, Mat4Transform.rotateAroundZ(angle));
    
    headTransform.setTransform(transform); 
    spotlightRoot.update(); 

    // 2. Update Light Source Position
    // Manual Fix for position (as per your code)
    float fixedX = -8f + 4f; 
    float fixedY = -0.5f + 10.5f; 
    float fixedZ = -8f;
    Vec3 pos = new Vec3(fixedX, fixedY, fixedZ);

    // 3. Direction Calculation
    float rad = (float)Math.toRadians(angle);
    
    // [FIX] dirX = +Math.sin to match model rotation
    // [FIX] dirY = -Math.cos to point DOWN
    float dirX = (float)Math.sin(rad); 
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
    
    SGTransformNode transRoot = new SGTransformNode("Transform Root", Mat4Transform.translate(-8f, -1f, -8f));
    
    SGNameNode baseNode = new SGNameNode("Base");
    Mat4 baseM = Mat4Transform.translate(0, 1f, 0);
    baseM = Mat4.multiply(baseM, Mat4Transform.scale(1f, 1f, 1f));
    SGTransformNode baseTransform = new SGTransformNode("Base Scale", baseM);
    SGModelNode baseShape = new SGModelNode("Base Shape", sphere);

    SGNameNode poleNode = new SGNameNode("Pole");
    Mat4 poleM = Mat4Transform.translate(0, 6f, 0); 
    poleM = Mat4.multiply(poleM, Mat4Transform.scale(0.5f, 10f, 0.5f)); 
    SGTransformNode poleTransform = new SGTransformNode("Pole Scale", poleM);
    SGModelNode poleShape = new SGModelNode("Pole Shape", sphere);

    SGNameNode armNode = new SGNameNode("Arm");
    Mat4 armM = Mat4Transform.translate(2f, 10.5f, 0f); 
    armM = Mat4.multiply(armM, Mat4Transform.scale(4f, 0.5f, 0.5f)); 
    SGTransformNode armTransform = new SGTransformNode("Arm Scale", armM);
    SGModelNode armShape = new SGModelNode("Arm Shape", sphere);

    SGNameNode headNode = new SGNameNode("Head");
    headTransform = new SGTransformNode("Head Rotation", Mat4Transform.translate(4f, 10.5f, 0f));
    
    Mat4 headScaleM = Mat4Transform.scale(1.5f, 1.5f, 1.5f);
    SGTransformNode headScale = new SGTransformNode("Head Scale", headScaleM);
    SGModelNode headShape = new SGModelNode("Head Shape", sphere);

    Mat4 bulbM = Mat4Transform.translate(0f, -0.6f, 0f); 
    bulbM = Mat4.multiply(bulbM, Mat4Transform.scale(0.8f, 0.5f, 0.8f));
    SGTransformNode bulbTrans = new SGTransformNode("Bulb Trans", bulbM);
    SGModelNode bulbShape = new SGModelNode("Bulb Shape", bulb);

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
          headTransform.addChild(bulbTrans); 
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
    Material material = new Material(new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), new Vec3(1f, 1f, 1f), 32.0f);
    material.setDiffuseMap(diffuse);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }
}