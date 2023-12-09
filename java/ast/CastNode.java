package ast;

import ast.visitor.ASTVisitor;
import compiler.Scope;

/**
 * Node for cast expressions
 * 
 * It has one children:
 * 
 * <code>expr</code>
 * 
 * for the return type of the cast type
**/
public class CastNode extends ExpressionNode {
	
	private ExpressionNode expr;
	private Scope.Type type;
	
	public CastNode(ExpressionNode expr, String type) {
		this.setExpr(expr);
		this.setType(getTypefromString(type)); //This node inherits its type from the left child
	}
		
	private Scope.InnerType getTypefromString(String s) {
		switch (s) {
		case "int" : return Scope.InnerType.INT; 
		case "float" : return Scope.InnerType.FLOAT;
		default : throw new Error ("Unrecognized type");
		}
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor) {
		return visitor.visit(this);
	}

	public ASTNode getExpr() {
		return expr;
	}

	private void setExpr(ExpressionNode expr) {
		this.expr = expr;
	}

	public Scope.Type getType() {
		//return new Scope.Type(type);
		return type;
	}

	private void setType(Scope.InnerType type) {
		this.type = new Scope.Type(type);
	}
	
}

