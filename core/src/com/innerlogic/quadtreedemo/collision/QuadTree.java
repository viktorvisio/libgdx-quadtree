package com.innerlogic.quadtreedemo.collision;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.innerlogic.quadtreedemo.entities.SpriteEntity;


/**
 * QuadTree class handling all operations and providing abstraction for programmers.
 * Note: This might be possible overhead. 
 * @author visio
 *
 */
public class QuadTree<T extends SpriteEntity> {
	
	
	QuadTreeNode<T> root_node; // Root node.
	
	Array<QuadTreeNode<T>> node_buffer; 
	Rectangle rect;
	
	/*int max_level;  // Not used, yet. */
	
	/**
	 * Simple constructor. 
	 * @param max_level Maximum depth our tree can reach.
	 * @param boundary  Main boundary used for root node.
	 */
	public QuadTree(int max_level, Rectangle boundary){
		//this.max_level = max_level;		
		root_node = new QuadTreeNode<T>(0, boundary);
		node_buffer = new Array<QuadTreeNode<T>>();
		rect = new Rectangle();
	}
	
	
	public void clear(){
		root_node.clear();
	}
	
	 
	public void insert(T entity){
		root_node.insert(entity);
	}
	 
	 /**
	  * Non-recursive retrieve. Need intensive profiling to determine if it's faster.
	  * It's possible to run this in another thread. 
	  * For example If we have different layers for collision or culling (multiple independent QuadTrees) we can resume
	  * separated threads which will retrieve objects thus saving time.
	  * 
	  * @param entitiesToReturn Array used to store retrieved entities
	  * @param entity Entity we are testing againts. Can be view boundary for render culling.
	  * @return
	  */
	 public void retrieve(Array<T> entitiesToReturn, T entity){
		 
		 QuadTreeNode<T> temp;
		 
		 rect.x = entity.getX();
		 rect.y = entity.getY();
		 rect.width = entity.getWidth();
		 rect.height = entity.getHeight();
		 
		 node_buffer.add(root_node);
		 
		 do{
			 
			 temp = node_buffer.pop();
			 temp.retrieve(node_buffer, entitiesToReturn, entity.getBoundingRectangle());
		 }while(node_buffer.size != 0);
		 
	}
	 
	public void render(ShapeRenderer shapeRenderer){
		root_node.render(shapeRenderer);
	}
	
}
