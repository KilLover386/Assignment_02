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
  private SGTransformNode bodyTransformNode;

  private SGTransformNode leftWingRoot;
  private SGTransformNode leftWingPivot;
  private SGTransformNode leftWingShapeTransform;

  private SGTransformNode rightWingRoot;
  private SGTransformNode rightWingPivot;
  private SGTransformNode rightWingShapeTransform;

  private double lastUpdateTime;
  private Vec3 lastPosition;
  private double flapPhase = 0.0;

  public Bee(GL3 gl, Light light, Camera camera, TextureLibrary textures) {
    this.position = new Vec3(0f, 5f, 0f);
    this.rotationAngle = 0f;
    this.currentHeading = 0f;
    this.light = light;
    this.camera = camera;
    buildBeeStructure(gl, textures);

    lastUpdateTime = System.currentTimeMillis() / 1000.0;
    lastPosition = new Vec3(position.x, position.y, position.z);
  }

  private void buildBeeStructure(GL3 gl, TextureLibrary textures) {
    beeRoot = new SGTransformNode("bee", Mat4Transform.translate(position.x, position.y, position.z));

    // --- BODY ---
    Model bodyModel = makeSphere(gl, textures.get("beeSkin"), "body");
    
    // Scale: (2, 3, 2). Local Y is the spine.
    Mat4 bodyTransform = Mat4Transform.scale(0.5f, 1.7f, 0.5f);     bodyTransformNode = new SGTransformNode("body_scale", bodyTransform);
    SGModelNode bodyShape = new SGModelNode("body_shape", bodyModel);

    beeRoot.addChild(bodyTransformNode);
    bodyTransformNode.addChild(bodyShape);

    // --- ANTENNAE ---
    Model blackModel = makeSphere(gl, textures.get("black1x1"), "antenna_part");

    // UPDATED: Smaller sizes
    float antBaseScaleX = 0.05f; 
    float antBaseScaleY = 0.4f; 
    float antBaseScaleZ = 0.05f;
    float antTipScale = 0.1f;  

    // UPDATED: Positioned higher (0.5 is the top of the body sphere)
    float antOffsetX = 0.2f;
    float antOffsetY = 0.5f; 
    float antOffsetZ = 0.15f; 

    // -- Left Antenna --
    SGTransformNode lAntRoot = new SGTransformNode("l_ant_root", 
        Mat4.multiply(Mat4Transform.translate(-antOffsetX, antOffsetY, antOffsetZ), 
                      Mat4Transform.rotateAroundZ(25))); 
    
    SGTransformNode lAntBaseTrans = new SGTransformNode("l_ant_base_trans", 
        Mat4.multiply(Mat4Transform.translate(0, 0.2f, 0), 
                      Mat4Transform.scale(antBaseScaleX, antBaseScaleY, antBaseScaleZ)));
    SGModelNode lAntBaseShape = new SGModelNode("l_ant_base_shape", blackModel);

    SGTransformNode lAntTipTrans = new SGTransformNode("l_ant_tip_trans",
        Mat4.multiply(Mat4Transform.translate(0, 0.45f, 0), 
                      Mat4Transform.scale(antTipScale, antTipScale, antTipScale)));
    SGModelNode lAntTipShape = new SGModelNode("l_ant_tip_shape", blackModel);

    bodyTransformNode.addChild(lAntRoot);
      lAntRoot.addChild(lAntBaseTrans);
        lAntBaseTrans.addChild(lAntBaseShape);
      lAntRoot.addChild(lAntTipTrans);
        lAntTipTrans.addChild(lAntTipShape);

    // -- Right Antenna --
    SGTransformNode rAntRoot = new SGTransformNode("r_ant_root", 
        Mat4.multiply(Mat4Transform.translate(antOffsetX, antOffsetY, antOffsetZ), 
                      Mat4Transform.rotateAroundZ(-25))); 
    
    SGTransformNode rAntBaseTrans = new SGTransformNode("r_ant_base_trans", 
        Mat4.multiply(Mat4Transform.translate(0, 0.2f, 0),
                      Mat4Transform.scale(antBaseScaleX, antBaseScaleY, antBaseScaleZ)));
    SGModelNode rAntBaseShape = new SGModelNode("r_ant_base_shape", blackModel);

    SGTransformNode rAntTipTrans = new SGTransformNode("r_ant_tip_trans",
        Mat4.multiply(Mat4Transform.translate(0, 0.45f, 0),
                      Mat4Transform.scale(antTipScale, antTipScale, antTipScale)));
    SGModelNode rAntTipShape = new SGModelNode("r_ant_tip_shape", blackModel);

    bodyTransformNode.addChild(rAntRoot);
      rAntRoot.addChild(rAntBaseTrans);
        rAntBaseTrans.addChild(rAntBaseShape);
      rAntRoot.addChild(rAntTipTrans);
        rAntTipTrans.addChild(rAntTipShape);


    // --- WINGS ---
    Model wingModel = makeSphere(gl, textures.get("white1x1"), "wing");

    float attachX = 0.5f;       
    float attachY = 0.0f;       

    float wingScaleX = 1.2f;    
    float wingScaleY = 0.1f;   
    float wingScaleZ = 0.3f;    
    float wingHalfLength = 0.6f;

    // Right Wing
    Mat4 rightRootMat = Mat4.multiply(Mat4Transform.translate(attachX, attachY, 0f), 
                                      Mat4Transform.rotateAroundX(90)); 
    rightWingRoot = new SGTransformNode("rightWing_root", rightRootMat);
    rightWingPivot = new SGTransformNode("rightWing_pivot", new Mat4(1));
    Mat4 rightWingShapeMat = Mat4.multiply(Mat4Transform.translate(wingHalfLength, 0f, 0f),
                                           Mat4Transform.scale(wingScaleX, wingScaleY, wingScaleZ));
    rightWingShapeTransform = new SGTransformNode("rightWing_shape_transform", rightWingShapeMat);
    SGModelNode rightWingShape = new SGModelNode("rightWing_shape", wingModel);

    bodyTransformNode.addChild(rightWingRoot);
      rightWingRoot.addChild(rightWingPivot);
        rightWingPivot.addChild(rightWingShapeTransform);
          rightWingShapeTransform.addChild(rightWingShape);

    // Left Wing
    Mat4 leftRootMat = Mat4.multiply(Mat4Transform.translate(-attachX, attachY, 0f), 
                                     Mat4Transform.rotateAroundX(90));
    leftWingRoot = new SGTransformNode("leftWing_root", leftRootMat);
    leftWingPivot = new SGTransformNode("leftWing_pivot", new Mat4(1));
    Mat4 leftWingShapeMat = Mat4.multiply(Mat4Transform.translate(-wingHalfLength, 0f, 0f),
                                          Mat4Transform.scale(wingScaleX, wingScaleY, wingScaleZ));
    leftWingShapeTransform = new SGTransformNode("leftWing_shape_transform", leftWingShapeMat);
    SGModelNode leftWingShape = new SGModelNode("leftWing_shape", wingModel);

    bodyTransformNode.addChild(leftWingRoot);
      leftWingRoot.addChild(leftWingPivot);
        leftWingPivot.addChild(leftWingShapeTransform);
          leftWingShapeTransform.addChild(leftWingShape);

    beeRoot.update();
  }

  private Model makeSphere(GL3 gl, Texture diffuse, String partName) {
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = new Mat4(1);
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_d.txt");
    Material material = new Material(new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.5f, 0.5f, 0.5f), 32.0f);
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(null);
    material.setEmissionMap(null);
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
      // UPDATED: Added + 180f to flip the direction
      currentHeading = (float)Math.toDegrees(Math.atan2(dx, dz)) + 180f;
    }
    
    updateTransform();
  }

  public void setRotationAngle(float angle) {
    this.rotationAngle = angle;
    updateTransform();
  }

  private void updateTransform() {
    Mat4 transform = Mat4Transform.translate(position);
    transform = Mat4.multiply(transform, Mat4Transform.rotateAroundY(currentHeading));
    transform = Mat4.multiply(transform, Mat4Transform.rotateAroundX(rotationAngle));

    beeRoot.setTransform(transform);

    // --- Animation ---
    double now = System.currentTimeMillis() / 1000.0;
    double dt = now - lastUpdateTime;
    if (dt <= 0) dt = 1e-6;

    double dx = position.x - lastPosition.x;
    double dy = position.y - lastPosition.y;
    double dz = position.z - lastPosition.z;
    double speed = Math.sqrt(dx*dx + dy*dy + dz*dz) / dt;

    double baseFlapRate = 50.0; 
    flapPhase += baseFlapRate * dt * (1.0 + speed*0.5); 

    // UPDATED: Max degree 80.0 for very high/low flapping
    double maxDeg = 80.0;
    double amplitudeDeg = Math.min(maxDeg, speed * 40.0 + 30.0); 
    double amplitudeRad = Math.toRadians(amplitudeDeg);
    double baseTiltRad = Math.toRadians(5.0);

    double flapOffset = amplitudeRad * Math.sin(flapPhase);

    rightWingPivot.setTransform(Mat4Transform.rotateAroundZ((float)(baseTiltRad + flapOffset)));
    leftWingPivot.setTransform(Mat4Transform.rotateAroundZ((float)(-baseTiltRad + -flapOffset)));

    lastUpdateTime = now;
    lastPosition.x = position.x;
    lastPosition.y = position.y;
    lastPosition.z = position.z;

    beeRoot.update();
  }

  public void render(GL3 gl) {
    beeRoot.draw(gl);
  }
}