package game;

public class Swarmling extends GameObject {
	static Swarmling lastInLine;
	static final float maxSpeed = 3.4f, maxAccel = 0.3f;
	static final float swarmlingDriftAccel = 1.5f;
	static final float attractRadius=90;
	static final float swarmlingRadius=5;
	//should be a magnitude of world radius
	static final float wanderingFactor=1000;
	static final float attackRadius = 100f;
	static float seed=0;
	Swarmling following = null;
	int followCooldown = 0; // how many frames until ready to follow again
	static int queueCooldown = 0; //how much frame should wait for the next swarmling to follow

	float leastDistance = 10000f;
	Obstacle target = null;
	int attackCooldownCount = 30;	
	int attackCooldown = (int)Math.random()*attackCooldownCount;
	
	Carryable carrying = null;
	float carryX, carryY; // swarmling's position relative to what it's carrying

	
	Swarmling(Sketch s, float ix, float iy) {
		sketch = s;
		x = ix;
		y = iy;
		dx = sketch.random(-1 * maxSpeed, maxSpeed);
		dy = sketch.random(-1 * maxSpeed, maxSpeed);
		radius = swarmlingRadius;
		avoidRadius = 10f;
		color = sketch.color(40, 65, 40);
	}
	
	public void follow(Swarmling s) {
		following = s;
		lastInLine = this;
		queueCooldown=30;
	}
	
	public void unfollow() {
		if (carrying != null) {
			for (int i = 0; i < carrying.carriedBy.size(); ++i) {
				if (carrying.carriedBy.get(i) == this) {
					carrying.carriedBy.remove(i);
					break;
				}
			}
			carrying = null;
			following = null;
		} else if (following != null) {
	        if (lastInLine == this) {
	        	lastInLine = following;
	        } else {
	        	// if this swarmling is in the middle of the line,
	        	// link up its follower to its following
	        	Swarmling s = lastInLine;
	        	while (s.following != this) {
	        		s = s.following;
	        	}
	        	s.following = following;
	        }
	        following = null;
	        followCooldown = 60;
	    }
	}
	
	public boolean update() {
		
		float ddx = 0, ddy = 0; //acceleration
		float avoidFactor = 0.25f;
		
		// Check for following/unfollowing.
		followCooldown = Sketch.max(0, followCooldown - 1);
		if (sketch.leader.leading &&
	            (followCooldown == 0) && 
	            (following == null) &&
	            (queueCooldown == 0) &&
	            (Sketch.dist(x, y, lastInLine.x, lastInLine.y) < attractRadius)){
			follow(lastInLine);
		} else if(following != null && !sketch.leader.leading){
			unfollow();
		}
		
		// Add follow vector.
		if (following != null) {
			ddx += (following.x - x) / 4;
			ddy += (following.y - y) / 4;
		}
		
		// Add friction drag.
		ddx -= dx / 20;
		ddy -= dy / 20;
		
		//closest target
		target = null;
		float targetDist = attackRadius;
		attackCooldown = Sketch.max(0, attackCooldown-1);
		
		//closest wandering enemy
		WanderingEnemy wanderingEnemy = null;
		float predateDist = WanderingEnemy.predateRadius;
		
		// Iterate through other GameObjects in the world,
		// checking for collision and movement influence
		for (int i = 0; i < sketch.world.contents.size(); ++i) {
			GameObject other = sketch.world.contents.get(i);
			if (other != this) {
				float distance = distTo(other);
				
				if ((following != null) && (carrying == null) && (other instanceof Carryable) && (distance <= 0)) {
					Carryable carrything = (Carryable) other;
					if (carrything.carryCap > carrything.carriedBy.size()) {
						carrything.carriedBy.add(this);
						carrying = carrything;
						carryX = x - carrything.x;
						carryY = y - carrything.y;
						if (lastInLine == this) {
							lastInLine = this.following;
						} else {
							for (Swarmling s = lastInLine; s != null; s = s.following) {
								if (s.following == this) {
									s.following = following;
									break;
								}
							}
						}
					}
				}
				
				// special interactions with obstacles
				if (other instanceof Obstacle) {
					// check if it can be our new target.
					if ((attackCooldown == 0) && (distance < targetDist)) {
					// death on collision
//					if (distance <= 0) {
//						unfollow();
//						sketch.world.contents.add(new Burst(sketch, x, y, color));
//						return false;
//						
//					// check if it can be our new target.
//					} else if ((distance < targetDist) && (carrying == null)) {
						target = (Obstacle) other;
						targetDist = distance;
					}
				}
				
				if (other instanceof WanderingEnemy){
					WanderingEnemy tmpEnemy = (WanderingEnemy)other;
					if(distance < predateDist && tmpEnemy.isAttacking==true){
						wanderingEnemy = tmpEnemy;
						predateDist = distance;
					}	
				}	
				// try to avoid whatever this is.
				if (distance < other.avoidRadius) {
					float centerDist = Sketch.dist(x, y, other.x, other.y);
					ddx += ((other.x - x) / centerDist) * (1 - (distance / avoidRadius)) / 4;
					ddy += ((other.y - y) / centerDist) * (1 - (distance / avoidRadius)) / 4;
				}
			}
		}
		
		// Add predate vector if we found a wandering enemy,
		if(wanderingEnemy != null){
			ddx += (wanderingEnemy.x - x) / (1+distTo(wanderingEnemy)/WanderingEnemy.predateRadius);
			ddy += (wanderingEnemy.y - y) / (1+distTo(wanderingEnemy)/WanderingEnemy.predateRadius);
		}
		
		
		// Avoid the leader
		float leaderDistance = distTo(sketch.leader);
		if (leaderDistance < avoidRadius) {
			float centerDist = Sketch.dist(x, y, sketch.leader.x, sketch.leader.y);
			ddx += ((x - sketch.leader.x) / centerDist) * (1 - (leaderDistance / avoidRadius)) / 4;
			ddy += ((y - sketch.leader.y) / centerDist) * (1 - (leaderDistance / avoidRadius)) / 4;
		}
		
		// Attack if we found a target.
		if ((target != null) && (attackCooldown == 0)) {
			new Projectile(sketch, this, target);
			attackCooldown = 30;
		}
		
		//- wandering behavior
		
		// Clamp and apply acceleration.
		float accel = Sketch.mag(ddx, ddy);
		if (accel > maxAccel) {
			ddx *= maxAccel / accel;
			ddy *= maxAccel / accel;
		}
		dx += ddx;
		dy += ddy;
		// Clamp and apply velocity.
		float speed = Sketch.mag(dx, dy);
		if (speed > maxSpeed) {
			dx *= maxSpeed / speed;
			dy *= maxSpeed / speed;
		}
		
		if (carrying == null) {
			x += dx;
			y += dy;
		} else {
			carrying.dx += dx * (1 / carrying.weight);
			carrying.dy += dy * (1 / carrying.weight);
			float carrySpeed = Sketch.mag(dx, dy);
			float maxCarrySpeed = maxSpeed / carrying.weight;
			if (carrySpeed > maxCarrySpeed) {
				carrying.dx *= maxCarrySpeed / carrySpeed;
				carrying.dy *= maxCarrySpeed / carrySpeed;
			}
			x = carrying.x + carryX;
			y = carrying.y + carryY;
		}

		return true;
	}
	
	public void draw(WorldView view) {
		super.draw(view);
		
		if (target != null) {
			float dist = Sketch.dist(x, y, target.x, target.y);
			float amtFromSwarmling = radius / dist;
			float amtFromTarget = target.radius / dist;
			float x1 = view.screenX(Sketch.lerp(x, target.x, amtFromSwarmling));
			float y1 = view.screenY(Sketch.lerp(y, target.y, amtFromSwarmling));
			float x2 = view.screenX(Sketch.lerp(target.x, x, amtFromTarget));
			float y2 = view.screenY(Sketch.lerp(target.y, y, amtFromTarget));
			sketch.stroke(Projectile.defaultColor);
			sketch.strokeWeight(2);
			sketch.line(x1, y1, x2, y2);
		}
	}
	
	public static void drawLine(WorldView view) {
		sketch.noFill();
		sketch.stroke(0, 0, 255);
		sketch.strokeWeight(2);
		sketch.beginShape();
		
		Swarmling tail = lastInLine;
		Swarmling head = sketch.leader;
		
		sketch.curveVertex(view.screenX(tail.x - (30 * tail.dx)), view.screenY(tail.y - (30 * tail.dx)));
		for (Swarmling s = lastInLine; s != null; s = s.following) {
			sketch.curveVertex(view.screenX(s.x), view.screenY(s.y));
		}
		sketch.curveVertex(view.screenX(head.x + (30 * head.dx)), view.screenY(head.y + (30 * head.dx)));
		
		sketch.endShape();
	}
}
