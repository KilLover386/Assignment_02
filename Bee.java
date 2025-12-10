import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Bee {

  private SGTransformNode beeRoot;
  private Vec3 position;
  private float rotationAngle;
  private float currentHeading;
  private Light light;
  private Camera camera;
  
  // Hierarchy nodes for animation
  private SGTransformNode bodyTransformNode;
  private SGTransformNode leftWingPivot, rightWingPivot;
  private SGTransformNode leftEyeTransform, rightEyeTransform; 

  private double lastUpdateTime;
  private double flapPhase = 0.0;
  private float targetEyeScale = 1.0f;
  private float currentEyeScale = 1.0f;

  public Bee(GL3 gl, Light light, Camera camera, TextureLibrary textures) {
    this.position = new Vec3(0f, 5f, 0f);
    this.rotationAngle = -90f; 
    this.currentHeading = 0f;
    this.light = light;
    this.camera = camera;
    buildBeeStructure(gl, textures);

    lastUpdateTime = System.currentTimeMillis() / 1000.0;
  }

  private void buildBeeStructure(GL3 gl, TextureLibrary textures) {
    beeRoot = new SGTransformNode("bee", Mat4Transform.translate(position.x, position.y, position.z));

    // --- BODY ---
    Model bodyModel = makeSphere(gl, textures.get("beeSkin"), "body");
    Mat4 bodyTransform = Mat4Transform.scale(0.5f, 1.2f, 0.5f); 
    bodyTransformNode = new SGTransformNode("body_scale", bodyTransform);
    SGModelNode bodyShape = new SGModelNode("body_shape", bodyModel);

    beeRoot.addChild(bodyTransformNode);
    bodyTransformNode.addChild(bodyShape);

    // --- TAIL (STINGER) ---
    // [FIX] Made much thinner (0.08) to look pointy. Kept connection at -0.5.
    Model blackModel = makeSphere(gl, textures.get("black1x1"), "black_part");
    SGTransformNode tailTrans = new SGTransformNode("tail", 
        Mat4.multiply(Mat4Transform.translate(0, -0.5f, 0.0f), 
                      Mat4Transform.scale(0.08f, 0.35f, 0.08f))); 
    SGModelNode tailShape = new SGModelNode("tail_shape", blackModel);
    bodyTransformNode.addChild(tailTrans);
    tailTrans.addChild(tailShape);

    // --- EYES ---
    Model eyeModel = makeSphere(gl, textures.get("white1x1"), "eye");
    // Left Eye
    SGTransformNode lEyeRoot = new SGTransformNode("l_eye_root", Mat4Transform.translate(-0.15f, 0.4f, 0.15f));
    leftEyeTransform = new SGTransformNode("l_eye_scale", Mat4Transform.scale(0.15f, 0.15f, 0.15f));
    SGModelNode lEyeShape = new SGModelNode("l_eye_shape", eyeModel);
    bodyTransformNode.addChild(lEyeRoot);
    lEyeRoot.addChild(leftEyeTransform);
    leftEyeTransform.addChild(lEyeShape);

    // Right Eye
    SGTransformNode rEyeRoot = new SGTransformNode("r_eye_root", Mat4Transform.translate(0.15f, 0.4f, 0.15f));
    rightEyeTransform = new SGTransformNode("r_eye_scale", Mat4Transform.scale(0.15f, 0.15f, 0.15f));
    SGModelNode rEyeShape = new SGModelNode("r_eye_shape", eyeModel);
    bodyTransformNode.addChild(rEyeRoot);
    rEyeRoot.addChild(rightEyeTransform);
    rightEyeTransform.addChild(rEyeShape);

    // --- ANTENNAE ---
    float antBaseScale = 0.05f; 
    float antLen = 0.3f;
    float tipScale = 0.08f; 

    // Left Antenna
    // [FIX] Moved up to 0.6f
    SGTransformNode lAntRoot = new SGTransformNode("l_ant_root", 
        Mat4.multiply(Mat4Transform.translate(-0.2f, 0.6f, 0.0f), 
                      Mat4Transform.rotateAroundZ(25)));
    SGTransformNode lAntShapeTrans = new SGTransformNode("l_ant_stick", Mat4Transform.scale(antBaseScale, antLen, antBaseScale));
    SGModelNode lAntShape = new SGModelNode("l_ant_stick_shape", blackModel);
    SGTransformNode lAntTipTrans = new SGTransformNode("l_ant_tip", 
        Mat4.multiply(Mat4Transform.translate(0.0f, 0.18f, 0.0f), 
                      Mat4Transform.scale(tipScale, tipScale, tipScale)));
    SGModelNode lAntTipShape = new SGModelNode("l_ant_tip_shape", blackModel);
    bodyTransformNode.addChild(lAntRoot);
    lAntRoot.addChild(lAntShapeTrans); lAntShapeTrans.addChild(lAntShape);
    lAntRoot.addChild(lAntTipTrans); lAntTipTrans.addChild(lAntTipShape);

    // Right Antenna
    // [FIX] Moved up to 0.6f
    SGTransformNode rAntRoot = new SGTransformNode("r_ant_root", 
        Mat4.multiply(Mat4Transform.translate(0.2f, 0.6f, 0.0f), 
                      Mat4Transform.rotateAroundZ(-25)));
    SGTransformNode rAntShapeTrans = new SGTransformNode("r_ant_stick", Mat4Transform.scale(antBaseScale, antLen, antBaseScale));
    SGModelNode rAntShape = new SGModelNode("r_ant_stick_shape", blackModel);
    SGTransformNode rAntTipTrans = new SGTransformNode("r_ant_tip", 
        Mat4.multiply(Mat4Transform.translate(0.0f, 0.18f, 0.0f), 
                      Mat4Transform.scale(tipScale, tipScale, tipScale)));
    SGModelNode rAntTipShape = new SGModelNode("r_ant_tip_shape", blackModel);
    bodyTransformNode.addChild(rAntRoot);
    rAntRoot.addChild(rAntShapeTrans); rAntShapeTrans.addChild(rAntShape);
    rAntRoot.addChild(rAntTipTrans); rAntTipTrans.addChild(rAntTipShape);


    // --- WINGS ---
    Model wingModel = makeSphere(gl, textures.get("white1x1"), "wing");
    float wingLen = 1.0f; float wingWidth = 0.4f; float wingThick = 0.05f; 
    
    // Right Wing
    SGTransformNode rAnchor = new SGTransformNode("r_wing_anchor", Mat4Transform.translate(0.25f, 0.2f, 0f));
    rightWingPivot = new SGTransformNode("r_wing_pivot", new Mat4(1));
    SGTransformNode rWingOffset = new SGTransformNode("r_wing_offset",
        Mat4.multiply(Mat4Transform.translate(wingLen*0.5f, 0f, 0f), 
                      Mat4Transform.scale(wingLen, wingWidth, wingThick)));
    SGModelNode rWingShape = new SGModelNode("r_wing_shape", wingModel);

    bodyTransformNode.addChild(rAnchor);
    rAnchor.addChild(rightWingPivot);
    rightWingPivot.addChild(rWingOffset);
    rWingOffset.addChild(rWingShape);

    // Left Wing
    SGTransformNode lAnchor = new SGTransformNode("l_wing_anchor", Mat4Transform.translate(-0.25f, 0.2f, 0f));
    leftWingPivot = new SGTransformNode("l_wing_pivot", new Mat4(1));
    SGTransformNode lWingOffset = new SGTransformNode("l_wing_offset",
        Mat4.multiply(Mat4Transform.translate(-wingLen*0.5f, 0f, 0f), 
                      Mat4Transform.scale(wingLen, wingWidth, wingThick)));
    SGModelNode lWingShape = new SGModelNode("l_wing_shape", wingModel);

    bodyTransformNode.addChild(lAnchor);
    lAnchor.addChild(leftWingPivot);
    leftWingPivot.addChild(lWingOffset);
    lWingOffset.addChild(lWingShape);

    beeRoot.update();
  }

  private Model makeSphere(GL3 gl, Texture diffuse, String partName) {
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    Material material = new Material(new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.5f, 0.5f, 0.5f), 32.0f);
    material.setDiffuseMap(diffuse);
    Renderer renderer = new Renderer();
    return new Model(partName, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  public void setPosition(float x, float y, float z) {
    float dx = x - position.x;
    float dz = z - position.z;
    position.x = x;
    position.y = y;
    position.z = z;
    if (Math.abs(dx) > 0.01f || Math.abs(dz) > 0.01f) {
      currentHeading = (float)Math.toDegrees(Math.atan2(dx, dz)) + 180f; 
    }
    updateTransform();
  }

  public void setRotationAngle(float angle) {
    this.rotationAngle = angle;
    updateTransform();
  }

  // [FIX] Adjusted target scale (1.7) so eyes don't get huge
  public void reactToProximity(boolean isClose) {
    targetEyeScale = isClose ? 1.7f : 1.0f; 
  }

  private void updateTransform() {
    Mat4 transform = Mat4Transform.translate(position); 

    transform = Mat4.multiply(transform, Mat4Transform.rotateAroundY(currentHeading));
    //transform = Mat4.multiply(transform, Mat4Transform.rotateAroundY(180)); 

    // [FIX] If the bee is upside down, checking the rotationAngle might help.
    // If it was 90, try -90 or 0 depending on your model. 
    // However, sticking to your request, the hierarchy rotation is here:
    transform = Mat4.multiply(transform, Mat4Transform.rotateAroundX(rotationAngle)); 
    
    beeRoot.setTransform(transform);

    double now = System.currentTimeMillis() / 1000.0;
    double dt = now - lastUpdateTime;
    if (dt <= 0) dt = 1e-6;

    double baseFlapRate = 20.0; 
    flapPhase += baseFlapRate * dt; 
    float maxFlapAngle = 60.0f; 
    float angle = maxFlapAngle * (float)Math.sin(flapPhase);

    rightWingPivot.setTransform(Mat4Transform.rotateAroundY(angle));
    leftWingPivot.setTransform(Mat4Transform.rotateAroundY(-angle));

    float scaleSpeed = 5.0f * (float)dt;
    if (currentEyeScale < targetEyeScale) currentEyeScale = Math.min(targetEyeScale, currentEyeScale + scaleSpeed);
    else if (currentEyeScale > targetEyeScale) currentEyeScale = Math.max(targetEyeScale, currentEyeScale - scaleSpeed);
    
    float eyeBaseSize = 0.15f;
    float finalScale = eyeBaseSize * currentEyeScale;
    leftEyeTransform.setTransform(Mat4Transform.scale(finalScale, finalScale, finalScale));
    rightEyeTransform.setTransform(Mat4Transform.scale(finalScale, finalScale, finalScale));

    lastUpdateTime = now;
    beeRoot.update();
  }

  public void render(GL3 gl) {
    beeRoot.draw(gl);
  }
  
  public Vec3 getPosition() { return position; }
}