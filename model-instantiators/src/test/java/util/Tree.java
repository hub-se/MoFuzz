package util;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {
    private Node<T> root;

    public Tree(T rootData) {
        root = new Node<T>(rootData);
    }
    
    public Node<T> getRoot(){
    	return this.root;
    }

    public static class Node<T> {
        private T data;
        private Node<T> parent;
        private List<Node<T>> children;
        
        public Node(T data) {
        	this.data = data;
        	this.parent = null;
        	this.children = new ArrayList<Node<T>>();
        }
        
        public Node(T data, Node<T> parent) {
        	this.data = data;
        	this.parent = parent;
        	this.children = new ArrayList<Node<T>>();
        	parent.addChild(this);
        }
        
        public void addChild(Node<T> child) {
        	this.children.add(child);
        }
        
        public T getData() {
        	return this.data;
        }
        
        public List<Node<T>> getChildren() {
        	return this.children;
        }
        
        @Override
        public String toString() {
        	return this.data.toString();
        }
    }
}