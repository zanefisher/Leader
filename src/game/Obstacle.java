package game;

public class Obstacle extends CircularGameObject {
	
	static float obstacleMinRadius = 40;
	static float obstacleMaxRadius = 200;
	static float obstacleMaxSpeed = 3.8f;
	static float obstacleMinSpeed = 0.6f;
	static float obstacleAvoidence = 80f;
	static int maxSwarmlingsGeneratedForDeadObstacle = 2;
	//float obstacleLife=1f;
	World world = null;
	//float raius=0;
	Obstacle(Sketch s, World w){
		sketch = s;
		color=sketch.color(255,255,255,255);
		objectAvoidence=obstacleAvoidence;
		world = w;
	}
	
	Obstacle(Sketch s,World w, float ix, float iy){
		sketch = s;
		x=ix;
		y=iy;
		color=sketch.color(255,255,255,255);
		objectAvoidence=obstacleAvoidence;
		world = w;
	}
	
	public void init(){
		radius = sketch.montecarlo((obstacleMaxRadius - obstacleMinRadius) / 2, (obstacleMaxRadius + obstacleMinRadius) / 2);
		//Sketch.println("monter: " + radius);
		float speed = sketch.random(obstacleMinSpeed, obstacleMaxSpeed) * obstacleMinRadius / radius;
		//Sketch.println("speed: " + speed);
		float radians = sketch.random(2) * Sketch.PI;
		obstacleLife = radius;
		//Sketch.println("radians: " + radians);
		x = Sketch.sin(radians) * (radius + world.innerRadius);		
		y = Sketch.cos(radians) * (radius + world.innerRadius);
		dx = Sketch.sin(radians) * speed * -1;
		dy = Sketch.cos(radians) * speed * -1;
		world.contents.add(this);
	}
	
	public boolean update(){
		

		
		x += dx;
		y += dy;
		if(Sketch.abs(sketch.world.camera.screenX(x))>(sketch.world.innerRadius + radius * 5) || (Sketch.abs(sketch.world.camera.screenY(y))> (sketch.world.innerRadius + radius * 5))){
			sketch.world.obstacleNumber-=1;
			return false;
		}
		
		//check if it has died
		if(obstacleLife <= 0f) {
			//Generate New Swarmlings
			for(int i=0; i<(int)maxSwarmlingsGeneratedForDeadObstacle*radius/obstacleMaxRadius; i++){
				float rx = x + sketch.random(radius);
				float ry = y + sketch.random(radius);
				Swarmling rs= new Swarmling(sketch, rx, ry);
				world.contents.add(rs);
			}
			sketch.world.obstacleNumber-=1;
			return false;
		}
		else{
			return true;
		}
		
		

	}
	
	public void draw(Camera camera){
		super.draw(camera);
		
	    sketch.noFill();
	    sketch.stroke(0, 0, 0, 255);
	    sketch.strokeWeight(6);
	    float halfArcLength = Sketch.PI * (1-obstacleLife / radius);
	    sketch.arc(camera.screenX(x), camera.screenY(y), radius*2, radius*2, Sketch.HALF_PI+halfArcLength, Sketch.TWO_PI+Sketch.HALF_PI - halfArcLength);
//	    Sketch.println("obstacleLife:"+obstacleLife);
	}
}

