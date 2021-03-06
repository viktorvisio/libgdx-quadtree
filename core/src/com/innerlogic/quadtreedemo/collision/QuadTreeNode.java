package com.innerlogic.quadtreedemo.collision;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.innerlogic.quadtreedemo.entities.SpriteEntity;

/**
 * Created with IntelliJ IDEA.
 * User: whita03
 * Date: 10/11/13
 * Time: 4:07 PM
 */
public class QuadTreeNode<T extends SpriteEntity> implements Poolable
{
    // Max number to store in this quad before subdividing
    public static final int MAX_ENTITIES = 4;
    private static final int MAX_LEVELS = 5;

    // Spatial bounds of this node
    private Rectangle boundary;

    // All entities in this quad
    private Array<T> entities;

    // The level of this node
    private int level;

    // Child QuadTreeNodes
    private Array<QuadTreeNode<T>> childNodes;

    // Scratch variables
    private static Vector2 BOUNDS_CENTER = null;

    public QuadTreeNode(int level, Rectangle boundary)
    {
        this.level = level;
        this.boundary = boundary;

        entities = new Array<T>(true, MAX_ENTITIES);

        // NW, NE, SE, SW (Clockwise)
        childNodes = new Array<QuadTreeNode<T>>(true, 4);

        if(BOUNDS_CENTER == null)
        {
            BOUNDS_CENTER = new Vector2();
        }
    }

    /**
     * Clear out all entities and null out all child nodes
     */
    public void clear()
    {
        // Clear the entities
        entities.clear();

        // Clear out each child node
        for(QuadTreeNode<T> currNode : childNodes)
        {
            if(currNode != null)
            {
                currNode.clear();
                currNode = null;
            }
        }

        // Lastly, clear out the nodes list
        childNodes.clear();
    }

    /**
     * Subdivide the node into 4 child nodes
     */
    private void subdivide()
    {
        float width_div2 = boundary.width / 2;
        float height_div2 = boundary.height / 2;
        float x = boundary.x;
        float y = boundary.y;
        
        int next_level = level + 1;

        // Create four child node which fully divide the boundary of this node
        Rectangle nwRect = new Rectangle(x, y + height_div2, width_div2, height_div2);
        childNodes.add(new QuadTreeNode<T>(next_level, nwRect));

        Rectangle neRect = new Rectangle(x + width_div2, y + height_div2, width_div2, height_div2);
        childNodes.add(new QuadTreeNode<T>(next_level, neRect));

        Rectangle seRect = new Rectangle(x + width_div2, y, width_div2, height_div2);
        childNodes.add(new QuadTreeNode<T>(next_level, seRect));

        Rectangle swRect = new Rectangle(x, y, width_div2, height_div2);
        childNodes.add(new QuadTreeNode<T>(next_level, swRect));
    }

    /**
     * Determine which node the entity belongs to. -1 means object cannot completely fit within a child node
     * and is part of the parent node
     */
    private int getIndex(SpriteEntity entity)
    {
        int index = -1;
        BOUNDS_CENTER = boundary.getCenter(BOUNDS_CENTER);

        // Object can completely fit within the top quadrants
        boolean topQuadrant = entity.getY() > BOUNDS_CENTER.y;

        // Object can completely fit within the bottom quadrants
        boolean bottomQuadrant = entity.getY() + entity.getHeight() < BOUNDS_CENTER.y;

        // Object can completely fit within the left quadrants
        if(entity.getX() < BOUNDS_CENTER.x && entity.getX() + entity.getWidth() < BOUNDS_CENTER.x)
        {
            if(topQuadrant)
            {
                index = 0;
            }
            else if(bottomQuadrant)
            {
                index = 3;
            }
        }
        // Object can completely fit within the right quadrants
        else if(entity.getX() > BOUNDS_CENTER.x)
        {
            if(topQuadrant)
            {
                index = 1;
            }
            else if(bottomQuadrant)
            {
                index = 2;
            }
        }

        // If we get here, the object can not fit completely in a child node, and will be part of the parent node
        return index;
    }

    /**
     * Insert an entity into the appropriate node, subdividing if necessary.
     *
     * @param entity
     */

    public void insert(T entity)
    {
        // If we have any child nodes, see if the entity could be contained completely inside of one
        // of them
        if(childNodes.size > 0)
        {
            int index = getIndex(entity);

            // If full containment is possible, recursively insert in that node.
            if(index != -1)
            {
                childNodes.get(index).insert(entity);

                return;
            }
        }

        // Add the entity to the list of entities for the node we are in
        entities.add(entity);

        // If we've exceeded the max number of entities for this node (And have more that we could subdivide),
        // attempt to subdivide and insert further
        if(entities.size > MAX_ENTITIES && level < MAX_LEVELS)
        {
            // Only subdivide if we haven't
            if(childNodes.size == 0)
            {
                subdivide();
            }

            int i = 0;
            while(i < entities.size)
            {
                // Move and insert what we can into the child nodes. If it can't be fully contained in the
                // child nodes, leave it at this level.
                int index = getIndex(entities.get(i));
                if(index != -1)
                {
                	T poppedEntity = entities.removeIndex(i);
                    QuadTreeNode<T> nodeToAddTo = childNodes.get(index);
                    nodeToAddTo.insert(poppedEntity);
                }
                else
                {
                    i++;
                }
            }
        }
    }

    /**
     * Return all entities that could collide with the given object
     */
    public Array<SpriteEntity> retrieve(Array<SpriteEntity> entitiesToReturn, SpriteEntity entity)
    {
        // If we have any child nodes, see if the entity could be contained completely inside of one
        // of them
        if(childNodes.size > 0)
        {
            int index = getIndex(entity);

            // If full containment is possible, recurse retrieval in that node.
            if(index != -1)
            {
                QuadTreeNode<T> nodeToRetrieveFrom = childNodes.get(index);
                nodeToRetrieveFrom.retrieve(entitiesToReturn, entity);
            }
        }

        // Add all the entities of the node we are in.
        entitiesToReturn.addAll(entities);

        return entitiesToReturn;
    }

    /**
     * Renders the boundaries of all the quad tree nodes in postorder depth traversal fashion recursively
     *
     * @param shapeRenderer
     */
    public void render(ShapeRenderer shapeRenderer)
    {
        // Attempt to render each child node
        for(QuadTreeNode<T> currNode : childNodes)
        {
            currNode.render(shapeRenderer);
        }

        // Set up the colors for the levels
        switch(level)
        {
            case 0:
            case 1:
                shapeRenderer.setColor(Color.ORANGE);
                break;

            case 2:
                shapeRenderer.setColor(Color.RED);
                break;

            case 3:
                shapeRenderer.setColor(Color.GREEN);
                break;

            case 4:
                shapeRenderer.setColor(Color.BLUE);
                break;

            case 5:
                shapeRenderer.setColor(Color.MAGENTA);
                break;
        }

        // Render the rect
        shapeRenderer.rect(boundary.x, boundary.y, boundary.width, boundary.height);
    }
    
    /**
     * Part of non-recursive retrieve.
     * This is where logic from master branch differs.
     * In here we are adding ALL nodes intersecting with our rectangle. 
     * As a result we will get higher number of retrieved objects but no possible collisions are omitted.
     * @param nodes
     * @param entitiesToRetrieve
     * @param rect
     */
	public void retrieve(Array<QuadTreeNode<T>> nodes, Array<T> entitiesToRetrieve, Rectangle rect){
		
		
		//  This will return every entity with overlaping boundary. Saves buffer space.
		/*
		int size = entities.size;
		for(int n = 0; n < size; n++){
			if(rect.overlaps(entities.get(n).getBoundingRectangle())){
				entitiesToRetrieve.add(entities.get(n));
			}
		}
		*/
		
		entitiesToRetrieve.addAll(entities); // Due to logic in GameScreen, I'll go with the same solution.
		
		// TODO Find better way to check for overlaps.
		if(childNodes.size != 0){
			
			if(childNodes.get(0).boundary.overlaps(rect)){
				nodes.add(childNodes.get(0));
			}
			
			if(childNodes.get(1).boundary.overlaps(rect)){
				nodes.add(childNodes.get(1));
			}
			
			if(childNodes.get(2).boundary.overlaps(rect)){
				nodes.add(childNodes.get(2));
			}
			
			if(childNodes.get(3).boundary.overlaps(rect)){
				nodes.add(childNodes.get(3));
			}
					
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
