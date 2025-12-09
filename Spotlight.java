import gmaths.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class Spotlight {

  private Model sphere;
  private SGNode spotlightRoot;
  private SGTransformNode headTransform; // Use this to rotate the head
  private Light light; // The actual light source object
  private float rotationAngle = 0f;

  public Spotlight(GL3 gl, Light light, Camera camera, TextureLibrary textures) {
    this.light = light;
    this.sphere = makeSphere(gl, light, camera, textures.get("specular_container")); // Use a metal-like texture
    makeSG();
  }

  public void update() {
    // 1. Animate the rotation (sweep)
    double time = System.currentTimeMillis() / 1000.0;
    float angle = 45.0f * (float)Math.sin(time); // Oscillate between -45 and 45 degrees
    headTransform.setTransform(Mat4Transform.rotateAroundY(angle));
    
    spotlightRoot.update(); // Update the scene graph matrices

    // 2. Extract world position and direction for the light source
    // The "head" node's world transform contains the position and orientation
    Mat4 worldT = headTransform.getWorldTransform();
    
    // Position is the translation column of the matrix (col 3)
    Vec3 pos = new Vec3(worldT.get(0, 3), worldT.get(1, 3), worldT.get(2, 3));
    
    // Direction: The light points down local Y. 
    // We multiply the local direction (0,-1,0) by the rotation part of the matrix.
    // (Simplification: applying the whole matrix to a direction vector with w=0)
    Vec4 dir4 = new Vec4(0f, -1f, 1f, 0f); // Pointing down and slightly forward
    // We need to normalize manually after transform if scales are involved, 
    // but here we just grab the logic.
    // Let's use a helper to get the forward vector from the matrix rotation
    
    // Simple approach: The light position is fixed relative to the head. 
    // The direction is rotating.
    // Let's just update the Light object directly.
    light.setPosition(pos);
    
    // Calculate direction based on the rotation angle
    // Default facing is Z, rotating around Y
    float dirX = (float)Math.sin(Math.toRadians(angle));
    float dirZ = (float)Math.cos(Math.toRadians(angle));
    // It points mostly down (-Y) but sweeps in X/Z
    Vec3 direction = new Vec3(dirX, -1.0f, 0.5f); 
    direction.normalize();
    light.setDirection(direction);
  }

  public void render(GL3 gl) {
    spotlightRoot.draw(gl);
  }

  private void makeSG() {
    // 1. The Root
    spotlightRoot = new SGNameNode("Spotlight Root");
    
    // 2. Transform to place it in the world (e.g., corner of the room)
    SGTransformNode transRoot = new SGTransformNode("Transform Root", Mat4Transform.translate(-8f, 0f, -8f));
    
    // 3. The Base/Pole (Static)
    SGNameNode poleNode = new SGNameNode("Pole");
    Mat4 m = Mat4Transform.scale(0.5f, 10f, 0.5f); // Tall thin pole
    m = Mat4.multiply(m, Mat4Transform.translate(0, 0.5f, 0)); // Move up so base is at 0
    SGTransformNode poleTransform = new SGTransformNode("Pole Scale", m);
    SGModelNode poleShape = new SGModelNode("Pole Shape", sphere);

    // 4. The Rotating Head (Dynamic)
    // Position it at the top of the pole (height 10)
    headTransform = new SGTransformNode("Head Rotation", Mat4Transform.translate(0, 10f, 0));
    
    // The lamp housing geometry
    Mat4 mHead = Mat4Transform.scale(1.5f, 1.5f, 1.5f);
    SGTransformNode headScale = new SGTransformNode("Head Scale", mHead);
    SGModelNode headShape = new SGModelNode("Head Shape", sphere);

    // Build the Tree
    spotlightRoot.addChild(transRoot);
      transRoot.addChild(poleNode);
        poleNode.addChild(poleTransform);
          poleTransform.addChild(poleShape);
        poleNode.addChild(headTransform); // Add head to top of pole
          headTransform.addChild(headScale);
            headScale.addChild(headShape);
            
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
}