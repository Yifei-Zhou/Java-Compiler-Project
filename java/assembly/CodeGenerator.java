package assembly;

import java.util.List;

import compiler.Scope.SymbolTableEntry;
import compiler.Scope.Type;
import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public char intTempPrefix = 't';
	static final public char floatTempPrefix = 'f';
	
	int unknownLabel;   //added in Step 3
	int loopLabel;
	int elseLabel;
	int outLabel;

	String currFunc;
	
	public CodeGenerator() {
		unknownLabel = 0;   //added in Step 3
		loopLabel = 0;
		elseLabel = 0;
		outLabel = 0;
		intRegCount = 0;		
		floatRegCount = 0;
	}

	public int getIntRegCount() {
		return intRegCount;
	}

	public int getFloatRegCount() {
		return floatRegCount;
	}
	
	/**
	 * Generate code for Variables
	 * 
	 * Create a code object that just holds a variable
	 * 
	 * Important: add a pointer from the code object to the symbol table entry
	 *            so we know how to generate code for it later (we'll need to find
	 *            the address)
	 * 
	 * Mark the code object as holding a variable, and also as an lval
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();

		return co;
	}

	/** Generate code for IntLiterals
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new Li(generateTemp(Scope.InnerType.INT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/** Generate code for FloatLiteras
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new FImm(generateTemp(Scope.InnerType.FLOAT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}
	
	/**
	 * Generate code for cast operations.
	 * 
	 * Step 0: create new code object
	 * 
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(CastNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();
		
		if(expr.lval) {
			expr = rvalify(expr);
		}
		
		co.code.addAll(expr.code);		
		if(node.getType().type != expr.getType().type) {
			switch(node.getType().type) {
			case INT: 
				String newtemp = generateTemp(Scope.InnerType.INT);
				Instruction i = new FmovI(expr.temp, newtemp);
				co.code.add(i);
				co.temp = newtemp;
				break;
			case FLOAT:
				newtemp = generateTemp(Scope.InnerType.FLOAT);
				i = new ImovF(expr.temp, newtemp);
				co.code.add(i);
				co.temp = newtemp;
				break;
			default:
				throw new Error("Wrong type");
			}
		}
		else {
			co.temp = expr.temp;
		}
		co.lval = false;
		expr.type = new Scope.Type(node.getType().type);
		co.type = new Scope.Type(node.getType().type);
		return co;
	}
	/**
	 * Generate code for binary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from left child
	 * Step 1a: if left child is an lval, add a load to get the data
	 * Step 2: add code from right child
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate binary operation using temps from left and right
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(BinaryOpNode node, CodeObject left, CodeObject right) {
		//step 0
		CodeObject co = new CodeObject();

		//step 1
		//step 1a
		if (left.lval == true){
			if(left.getSTE() != null) {
				if(left.getSTE().isLocal()) {
					co.code.addAll(left.code);
					switch(left.getType().type) {
						case INT: left.newtemp = generateTemp(Scope.InnerType.INT);
							Instruction load = new Lw(left.newtemp, "fp", String.valueOf(left.getSTE().addressToString()));
							co.code.add(load);
							break;
						case FLOAT: left.newtemp = generateTemp(Scope.InnerType.FLOAT);
							load = new Flw(left.newtemp, "fp", String.valueOf(left.getSTE().addressToString()));
							co.code.add(load);
							break;
						case PTR: left.newtemp = generateTemp(Scope.InnerType.INT);
							load = new Lw(left.newtemp, "fp", String.valueOf(left.getSTE().addressToString()));
							co.code.add(load);
							break;
						default: 
							throw new Error("Wrong type");
					}
				} else {
					left = rvalify(left); //Added in Step 4
					co.code.addAll(left.code);
					switch(left.getType().type) {
					case INT: left.newtemp = left.temp;
						break;
					case FLOAT: left.newtemp = left.temp;
						break;
					case PTR: left.newtemp = left.temp;
						break;
					default: 
						throw new Error("Wrong type");
					}
				}
			}
			else {
				left = rvalify(left); //Added in Step 4
				co.code.addAll(left.code);
				left.newtemp = left.temp;
			}
		}
		else {
			co.code.addAll(left.code);
			left.newtemp = left.temp;
		}
		
		//step 2
		
		if (right.lval == true){
			if(right.getSTE() != null) {
				if(right.getSTE().isLocal()) {
					co.code.addAll(right.code);
					switch(right.getType().type) {
						case INT: right.newtemp = generateTemp(Scope.InnerType.INT);
							Instruction load = new Lw(right.newtemp, "fp", String.valueOf(right.getSTE().addressToString()));
							co.code.add(load);
							break;
						case FLOAT: right.newtemp = generateTemp(Scope.InnerType.FLOAT);
							load = new Flw(right.newtemp, "fp", String.valueOf(right.getSTE().addressToString()));
							co.code.add(load);
							break;
						case PTR: right.newtemp = generateTemp(Scope.InnerType.INT);
							load = new Lw(right.newtemp, "fp", String.valueOf(right.getSTE().addressToString()));
							co.code.add(load);
							break;
						default: 
							throw new Error("Wrong type");
					}
				} else {
					right = rvalify(right); //Added in Step 4
					co.code.addAll(right.code);
					switch(right.getType().type) {
					case INT: right.newtemp = right.temp;
						break;
					case FLOAT: right.newtemp = right.temp;
						break;
					case PTR: right.newtemp = right.temp;
						break;
					default: 
						throw new Error("Wrong type");
					}
				}
			}
			else {
				right = rvalify(right); //Added in Step 4
				co.code.addAll(right.code);
				right.newtemp = right.temp;
			}
		}
		else {
			co.code.addAll(right.code);
			right.newtemp = right.temp;
		}
		

		
		switch (left.getType().type) {
			case INT:
				if(right.getType().type == left.getType().type) {
					switch (node.getOp()){
					case ADD:
						String newtemp = generateTemp(Scope.InnerType.INT);
						Instruction binaryoperation = new Add(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case SUB:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Sub(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case DIV:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Div(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case MUL:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Mul(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					default:
						throw new Error("Wrong Binary OpCode");
					}
				}
				else {
					switch (node.getOp()){
					case ADD:
						String newtemp = generateTemp(Scope.InnerType.FLOAT);
						String newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						Instruction casti = new ImovF(left.newtemp, newtempsrc);
						co.code.add(casti);
						Instruction binaryoperation = new FAdd(newtempsrc, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case SUB:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(left.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FSub(newtempsrc, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case DIV:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(left.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FDiv(newtempsrc, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case MUL:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(left.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FMul(newtempsrc, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					default:
						throw new Error("Wrong Binary OpCode");
					}
				}
				break;
			case PTR:
				switch (node.getOp()){
					case ADD:
						String newtemp = generateTemp(Scope.InnerType.INT);
						Instruction binaryoperation = new Add(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case SUB:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Sub(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case DIV:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Div(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case MUL:
						newtemp = generateTemp(Scope.InnerType.INT);
						binaryoperation = new Mul(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					default:
						throw new Error("Wrong Binary OpCode");
				}
				break;
			case FLOAT:
				if(right.getType().type == left.getType().type) {
					switch (node.getOp()){
					case ADD:
						String newtemp = generateTemp(Scope.InnerType.FLOAT);
						Instruction binaryoperation = new FAdd(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case SUB:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						binaryoperation = new FSub(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case DIV:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						binaryoperation = new FDiv(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case MUL:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						binaryoperation = new FMul(left.newtemp, right.newtemp, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					default:
						throw new Error("Wrong Binary OpCode");
					}
				}
				else {
					switch (node.getOp()){
					case ADD:
						String newtemp = generateTemp(Scope.InnerType.FLOAT);
						String newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						Instruction casti = new ImovF(right.newtemp, newtempsrc);
						co.code.add(casti);
						Instruction binaryoperation = new FAdd(left.newtemp, newtempsrc, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case SUB:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(right.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FSub(left.newtemp, newtempsrc, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case DIV:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(right.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FDiv(left.newtemp, newtempsrc, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					case MUL:
						newtemp = generateTemp(Scope.InnerType.FLOAT);
						newtempsrc = generateTemp(Scope.InnerType.FLOAT);
						casti = new ImovF(right.newtemp, newtempsrc);
						co.code.add(casti);
						binaryoperation = new FMul(left.newtemp, newtempsrc, newtemp);
						co.code.add(binaryoperation);
						co.temp = newtemp;
						break;
					default:
						throw new Error("Wrong Binary OpCode");
					}
				}
				
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		//System.err.println("Binary:");
		if(left.getType().type != right.getType().type) {	
			//System.err.println("test1");
			//System.err.println(left.getType().type);
			//System.err.println(right.getType().type);
			co.type = new Scope.Type(Scope.InnerType.FLOAT);
		}
		else {
			//System.err.println("test2");
			//System.err.println(left.getType().type);
			//System.err.println(right.getType().type);
			co.type = left.getType();
		}
		co.lval = false; //might buggy!!!!!!

		return co;
	}

	/**
	 * Generate code for unary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from child expression
	 * Step 1a: if child is an lval, add a load to get the data
	 * Step 2: generate instruction to perform unary operation
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();

		if (expr.getSTE() != null) {
			expr.code.addAll(generateAddrFromVariable(expr));
		}
		co.code.addAll(expr.code);
		expr.newtemp = expr.temp;
		if (expr.lval == true) {
			switch(expr.getType().type) {
				case INT: expr.newtemp = generateTemp(Scope.InnerType.INT);
					Instruction load = new Lw(expr.newtemp, expr.temp, "0");
					co.code.add(load);
					break;
				case FLOAT: expr.newtemp = generateTemp(Scope.InnerType.FLOAT);
					load = new Flw(expr.newtemp, expr.temp, "0");
					co.code.add(load);
					break;
				default: 
					throw new Error("Wrong type");
			}
		}

		switch (expr.getType().type) {
			case INT:
				switch (node.getOp()){
					case NEG:
						Instruction unaryoperation = new Neg(expr.newtemp, generateTemp(Scope.InnerType.INT));
						co.code.add(unaryoperation);
						co.temp = unaryoperation.getDest();
						break;
					default:
						throw new Error("Wrong Unary OpCode");
				}
				break;
			case FLOAT:
				switch (node.getOp()){
					case NEG:
						Instruction unaryoperation = new FNeg(expr.newtemp, generateTemp(Scope.InnerType.FLOAT));
						co.code.add(unaryoperation);
						co.temp = unaryoperation.getDest();
						break;
					default:
						throw new Error("Wrong Unary OpCode");
				}
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		co.type = expr.getType();
		co.lval = false;

		return co;
	}

	/**
	 * Generate code for assignment statements
	 * 
	 * Step 0: create new code object
	 * Step 1: if LHS is a variable, generate a load instruction to get the address into a register
	 * Step 1a: add code from LHS of assignment (make sure it results in an lval!)
	 * Step 2: add code from RHS of assignment
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate store
	 * 
	 * Hint: it is going to be easiest to just generate a store with a 0 immediate
	 * offset, and the complete store address in a register:
	 * 
	 * sw rhs 0(lhs)
	 */
	@Override
	protected CodeObject postprocess(AssignNode node, CodeObject left,
			CodeObject right) {
		
		CodeObject co = new CodeObject();

		assert(left.lval == true); //left hand side had better hold an address

		//step 1a
		if (left.getSTE() != null) {
			if (left.getSTE().isLocal()) { //if left is local use addi + sw. apply optimization sw

			}
			else {  //if left is global use la
				left.code.addAll(generateAddrFromVariable(left)); //LA:global or Addi fp: local
				left.temp = left.code.getLast().getDest();
			}
		}
		//step 1b
		co.code.addAll(left.code);
		co.lval = true;
		
		//step 2
		if (right.lval == true) {
			right = rvalify(right);
		}
		co.code.addAll(right.code);
		//step 3
		if(left.getSTE() != null) {
			switch(left.getType().type) {
			case INT: 
				if(right.getType().type == left.getType().type) {
					Instruction store = new Sw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				else {
					//System.err.println(left.getType().type);
					//System.err.println(right.getType().type);
					String newtemp = generateTemp(Scope.InnerType.INT);
					left.temp = newtemp;
					left.type = new Scope.Type(Scope.InnerType.INT);
					
					//System.err.println(left.getType().type);
					//System.err.println(right.getType().type);
					Instruction casti = new FmovI (right.temp, newtemp);
					co.code.add(casti);
					Instruction store = new Sw(newtemp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				break;
			case FLOAT: 
				if(right.getType().type == left.getType().type) {
					Instruction store = new Fsw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				else {
					String newtemp = generateTemp(Scope.InnerType.FLOAT);
					left.temp = newtemp;
					left.type = new Scope.Type(Scope.InnerType.FLOAT);
					Instruction casti = new ImovF (right.temp, newtemp);
					co.code.add(casti);
					Instruction store = new Fsw(newtemp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				break;
			case PTR:
				if(right.getType().type == Scope.InnerType.INT) {
					Instruction store = new Sw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				else if (right.getType().type == Scope.InnerType.FLOAT){
					Instruction store = new Fsw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				else if (right.getType().type == Scope.InnerType.INFER){
					Instruction store = new Sw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				else {
					Instruction store = new Sw(right.temp, "fp", left.getSTE().addressToString());
					co.code.add(store);
				}
				break;				
			default: 
				throw new Error("Wrong Type");
			}
		}
		//Added by Yifei
		else {
			if(right.getType().type == Scope.InnerType.INT) {
				Instruction store = new Sw(right.temp, left.temp, "0");
				co.code.add(store);
			}
			else if (right.getType().type == Scope.InnerType.FLOAT){
				Instruction store = new Fsw(right.temp, left.temp, "0");
				co.code.add(store);
			}
			else if (right.getType().type == Scope.InnerType.INFER){
				Instruction store = new Sw(right.temp, left.temp, "0");
				co.code.add(store);
			}
			else {
				Instruction store = new Sw(right.temp, left.temp, "0");
				co.code.add(store);
			}
		}
		
		co.temp = left.temp;
		//co.type = Type.pointerToType(right.type);
		co.lval = true;
		return co;
	}

	/**
	 * Add together all the lists of instructions generated by the children
	 */
	@Override
	protected CodeObject postprocess(StatementListNode node,
			List<CodeObject> statements) {
		CodeObject co = new CodeObject();
		//add the code from each individual statement
		for (CodeObject subcode : statements) {
			co.code.addAll(subcode.code);
		}
		co.type = null; //set to null to trigger errors
		return co;
	}
	
	/**
	 * Generate code for read
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from VarNode (make sure it's an lval)
	 * Step 2: generate GetI instruction, storing into temp
	 * Step 3: generate store, to store temp in variable
	 */
	@Override
	protected CodeObject postprocess(ReadNode node, CodeObject var) {
		
		//Step 0
		CodeObject co = new CodeObject();

		//Generating code for read(id)
		assert(var.getSTE() != null); //var had better be a variable

		InstructionList il = new InstructionList();
		switch(node.getType().type) {
			case INT: 
				//Code to generate if INT:
				//geti tmp
				//if var is global: la tmp', <var>; sw tmp 0(tmp')
				//if var is local: sw tmp offset(fp)
				Instruction geti = new GetI(generateTemp(Scope.InnerType.INT));
				il.add(geti);
				InstructionList store = new InstructionList();
				if (var.getSTE().isLocal()) {
					store.add(new Sw(geti.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					store.addAll(generateAddrFromVariable(var));
					store.add(new Sw(geti.getDest(), store.getLast().getDest(), "0"));
				}
				il.addAll(store);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf tmp
				//if var is global: la tmp', <var>; fsw tmp 0(tmp')
				//if var is local: fsw tmp offset(fp)
				Instruction getf = new GetF(generateTemp(Scope.InnerType.FLOAT));
				il.add(getf);
				InstructionList fstore = new InstructionList();
				if (var.getSTE().isLocal()) {
					fstore.add(new Fsw(getf.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					fstore.addAll(generateAddrFromVariable(var));
					fstore.add(new Fsw(getf.getDest(), fstore.getLast().getDest(), "0"));
				}
				il.addAll(fstore);
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		co.code.addAll(il);

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * Generate code for print
	 * 
	 * Step 0: create new code object
	 * 
	 * If printing a string:
	 * Step 1: add code from expression to be printed (make sure it's an lval)
	 * Step 2: generate a PutS instruction printing the result of the expression
	 * 
	 * If printing an integer:
	 * Step 1: add code from the expression to be printed
	 * Step 1a: if it's an lval, generate a load to get the data
	 * Step 2: Generate PutI that prints the temporary holding the expression
	 */
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType().type == Scope.InnerType.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);
			
			System.out.println("; generating code to print " + expr.getSTE());

			//Get the address of the variable
			InstructionList addrCo = generateAddrFromVariable(expr);
			co.code.addAll(addrCo);

			//Step 2:
			Instruction write = new PutS(addrCo.getLast().getDest());
			co.code.add(write);
		} else {
			//Step 1a:
			//if expr is an lval, load from it
			if (expr.lval == true) {
				expr = rvalify(expr);
			}
			
			//Step 1:
			co.code.addAll(expr.code);

			//Step 2:
			//if type of writenode is int, use puti, if float, use putf
			Instruction write = null;
			switch(node.getWriteExpr().getType().type) {
			case STRING: throw new Error("Shouldn't have a STRING here");
			case INT: 
			case PTR: //should work the same way for pointers
				write = new PutI(expr.temp); break;
			case FLOAT: write = new PutF(expr.temp); break;
			default: throw new Error("WriteNode has a weird type");
			}

			co.code.add(write);
		}

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Generating an instruction sequence for a conditional expression
	 * 
	 * Implement this however you like. One suggestion:
	 *
	 * Create the code for the left and right side of the conditional, but defer
	 * generating the branch until you process IfStatementNode or WhileNode (since you
	 * do not know the labels yet). Modify CodeObject so you can save the necessary
	 * information to generate the branch instruction in IfStatementNode or WhileNode
	 * 
	 * Alternate idea 1:
	 * 
	 * Don't do anything as part of CodeGenerator. Create a new visitor class
	 * that you invoke *within* your processing of IfStatementNode or WhileNode
	 * 
	 * Alternate idea 2:
	 * 
	 * Create the branch instruction in this function, then tweak it as necessary in
	 * IfStatementNode or WhileNode
	 * 
	 * Hint: you may need to preserve extra information in the returned CodeObject to
	 * make sure you know the type of branch code to generate (int vs float)
	 */
	@Override
	protected CodeObject postprocess(CondNode node, CodeObject left, CodeObject right) {
		CodeObject co = new CodeObject();

		//step 1: if left is a variable
		if (left.getSTE() != null) {
			if (left.getSTE().isLocal()) {
				
			}
			else {
				left.code.addAll(generateAddrFromVariable(left));
			}
		}
		co.code.addAll(left.code);
		//step 1a: if left is a lvalue then add a load to code object
		left.newtemp = left.temp;
		if(left.getSTE()!=null) {
			if (left.lval == true){
				if(left.getSTE().isLocal()) {
					switch(left.getType().type) {
					case INT: left.newtemp = generateTemp(Scope.InnerType.INT);
						Instruction load = new Lw(left.newtemp, "fp", left.getSTE().addressToString());
						co.code.add(load);
						break;
					case FLOAT: left.newtemp = generateTemp(Scope.InnerType.FLOAT);
						load = new Flw(left.newtemp, "fp", left.getSTE().addressToString());
						co.code.add(load);
						break;
					default: 
						throw new Error("Wrong type");
					}
				}
				else {
					switch(left.getType().type) {
					case INT: left.newtemp = generateTemp(Scope.InnerType.INT);
						Instruction load = new Lw(left.newtemp, left.code.getLast().getDest(), "0");
						co.code.add(load);
						break;
					case FLOAT: left.newtemp = generateTemp(Scope.InnerType.FLOAT);
						load = new Flw(left.newtemp, left.code.getLast().getDest(), "0");
						co.code.add(load);
						break;
					default: 
						throw new Error("Wrong type");
					}
				}
			}
		}
		else {
			switch(left.getType().type) {
			case INT: left.newtemp = generateTemp(Scope.InnerType.INT);
				Instruction load = new Lw(left.newtemp, left.temp, "0");
				co.code.add(load);
				break;
			case FLOAT: left.newtemp = generateTemp(Scope.InnerType.FLOAT);
				load = new Flw(left.newtemp, left.temp, "0");
				co.code.add(load);
				break;
			default: 
				throw new Error("Wrong type");
			}
		}
		
		
		
		//step 2: if right is a variable then get address
		if (right.getSTE() != null) {
				right.code.addAll(generateAddrFromVariable(right));
		}
		
		co.code.addAll(right.code);
		//step 2a: if right is a lvalue then add a load to code object
		right.temp = right.code.getLast().getDest();
		right.newtemp = right.temp;
		if (right.lval == true) {
			switch(right.getType().type) {
				case INT: right.newtemp = generateTemp(Scope.InnerType.INT);
					Instruction load = new Lw(right.newtemp, right.temp, "0");
					co.code.add(load);
					break;
				case FLOAT: right.newtemp = generateTemp(Scope.InnerType.FLOAT);
					load = new Flw(right.newtemp, right.temp, "0");
					co.code.add(load);
					break;
				default: 
					throw new Error("Wrong type");
			}
		}		
				
		switch (left.getType().type) {
		case INT:
			switch (node.getReversedOp()){
				case EQ:
					String newlabel = generateUnknownLabel();
					Instruction condoperation = new Beq(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				case NE:
					newlabel = generateUnknownLabel();
					condoperation = new Bne(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				case LT:
					newlabel = generateUnknownLabel();
					condoperation = new Blt(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				case LE:
					newlabel = generateUnknownLabel();
					condoperation = new Ble(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				case GT:
					newlabel = generateUnknownLabel();
					condoperation = new Bgt(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				case GE:
					newlabel = generateUnknownLabel();
					condoperation = new Bge(left.newtemp, right.newtemp, newlabel);
					co.code.add(condoperation);
					break;
				default:
					throw new Error("Wrong Cond OpCode");
			}
			break;
		case FLOAT:
			switch (node.getReversedOp()){
			case EQ:
				String newlabel = generateUnknownLabel();
				Instruction condoperation = new Feq(left.newtemp, right.newtemp, newlabel);
				co.code.add(condoperation);
				break;
			case NE:
				newlabel = generateUnknownLabel();
				condoperation = new Bne(left.newtemp, right.newtemp, newlabel);
				co.code.add(condoperation);
				break;
			case LT:
				newlabel = generateUnknownLabel();
				condoperation = new Flt(left.newtemp, right.newtemp, generateTemp(Scope.InnerType.INT));
				co.code.add(condoperation);
				condoperation = new Bne(generateTemp(Scope.InnerType.INT), "x0", newlabel);
				co.code.add(condoperation);
				break;
			case LE:
				newlabel = generateUnknownLabel();
				condoperation = new Fle(left.newtemp, right.newtemp, generateTemp(Scope.InnerType.INT));
				co.code.add(condoperation);
				condoperation = new Beq(generateTemp(Scope.InnerType.INT), "x0", newlabel);
				co.code.add(condoperation);
				break;
			case GT:
				newlabel = generateUnknownLabel();
				condoperation = new Fle(left.newtemp, right.newtemp, generateTemp(Scope.InnerType.INT));
				co.code.add(condoperation);
				condoperation = new Bne(generateTemp(Scope.InnerType.INT), "x0", newlabel);
				co.code.add(condoperation);
				break;
			case GE:
				newlabel = generateUnknownLabel();
				String newtemp = generateTemp(Scope.InnerType.INT);
				condoperation = new Flt(left.newtemp, right.newtemp, newtemp);
				co.code.add(condoperation);
				condoperation = new Beq(newtemp, "x0", newlabel);
				co.code.add(condoperation);
				break;
				default:
					throw new Error("Wrong Cond OpCode");
			}
			break;
		default:
			throw new Error("Shouldn't read into other variable");
	}
		co.type = left.getType();
		co.lval = true; //might buggy!!!!!!
		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		<cond code>
	 *		<flipped branch> elseLabel
	 *		<then code>
	 *		j outLabel
	 *		elseLabel:
	 *		<else code>
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();
		
		//step 1
		String newelselabel = generateElseLabel();
		String newoutlabel = generateOutLabel();
		
		//step 2
		
		
		//find cond code and change the unknown label to else label we just created
		Integer lastindex = cond.code.nodes.size()-1; //get last element's index in cond codelist
		Instruction condop = cond.code.nodes.get(lastindex);
		cond.code.nodes.remove(condop);
		
		if(elist.code.isEmpty() == false) {  //if this IfStatementNode has else part
			condop.label = newelselabel;  //changed intruction.java to make instruction.label public
		}
		else {
			condop.label = newoutlabel;  //changed intruction.java to make instruction.label public
		}
		cond.code.add(condop);
		
		//add code from cond node
		co.code.addAll(cond.code);
		
		//branch already generated in cond so skip step 3
		//step 4
		if(elist.code.isEmpty() == false) {  //if this IfStatementNode has else part
			Instruction jump = new J(newoutlabel);
			tlist.code.add(jump);
			Instruction elselabel = new Label(newelselabel);
			tlist.code.add(elselabel);
		}
		Instruction outlabel = new Label(newoutlabel);
		elist.code.add(outlabel);
		
		//step 5
		
		co.code.addAll(tlist.code);
		co.code.addAll(elist.code);
		
		co.temp = null;
		co.lval = false;
		co.type = null; //set to null to trigger errors
		

		return co;
	}

		/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		loopLabel:
	 *		<cond code>
	 *		<flipped branch> outLabel
	 *		<body code>
	 *		j loopLabel
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(WhileNode node, CodeObject cond, CodeObject slist) {
		//Step 0:
		CodeObject co = new CodeObject();
		
		//step 1
		String newlooplabel = generateLoopLabel();
		String newoutlabel = generateOutLabel();
		
		//step 2
		//find cond code and change the unknown label to loop label we just created
		Integer lastindex = cond.code.nodes.size()-1; //get last element's index in cond codelist
		Instruction condop = cond.code.nodes.get(lastindex);
		
		
		cond.code.nodes.remove(condop);
		
		
		condop.label = newoutlabel;  //changed intruction.java to make instruction.label public
		Instruction looplabel = new Label(newlooplabel);    //generate new instruction loop_x in front of all cond code
		
		cond.code.add(condop);
		
		CodeObject tempco = new CodeObject();
		tempco.code.add(looplabel);
		tempco.code.addAll(cond.code);		
		
		//add code from cond node
		co.code.addAll(tempco.code);
		
		//branch already generated in cond so skip step 3
		//step 4 & 5
		co.code.addAll(slist.code);
		
		Instruction jump = new J(newlooplabel);
		co.code.add(jump);
		
		Instruction outlabel = new Label(newoutlabel);
		co.code.add(outlabel);
		
		co.temp = null;
		co.lval = false;
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generating code for returns
	 * 
	 * Step 0: Generate new code object
	 * 
	 * Step 1: Add retExpr code to code object (rvalify if necessary)
	 * 
	 * Step 2: Store result of retExpr in appropriate place on stack (fp + 8)
	 * 
	 * Step 3: Jump to out label (use @link{generateFunctionOutLabel()})
	 */
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		
		//step 0
		CodeObject co = new CodeObject();
		
		//step 1
		if(node.getFuncSymbol().getType().type!=Scope.InnerType.VOID) {
			if(retExpr.lval) {
				retExpr = rvalify(retExpr);
			}
		
			co.code.addAll(retExpr.code);
		
			//step 2
			switch (retExpr.getType().type) {
			case INT:
				Instruction store = new Sw(retExpr.temp, "fp", "8"); 
				co.code.add(store);
				break;
			case FLOAT:
				store = new Fsw(retExpr.temp, "fp", "8"); 
				co.code.add(store);
				break;
			case PTR:
				store = new Sw(retExpr.temp, "fp", "8"); 
				co.code.add(store);
				break;
			case VOID:
				break;
			default:
				throw new Error("Wrong Call Type");
			}
		
		
			//step 3
			String newoutlabel = generateFunctionOutLabel();
			Instruction jump = new J(newoutlabel);
			co.code.add(jump);
		}
			
			co.temp = null;
			co.lval = false;
			co.type = null;
		

		return co;
	}

	@Override
	protected void preprocess(FunctionNode node) {
		// Generate function label information, used for other labels inside function
		currFunc = node.getFuncName();

		//reset register counts; each function uses new registers!
		intRegCount = 0;
		floatRegCount = 0;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generate code for functions
	 * 
	 * Step 1: add the label for the beginning of the function
	 * 
	 * Step 2: manage frame  pointer
	 * 			a. Save old frame pointer
	 * 			b. Move frame pointer to point to base of activation record (current sp)
	 * 			c. Update stack pointer
	 * 
	 * Step 3: allocate new stack frame (use scope infromation from FunctionNode)
	 * 
	 * Step 4: save registers on stack (Can inspect intRegCount and floatRegCount to know what to save)
	 * 
	 * Step 5: add the code from the function body
	 * 
	 * Step 6: add post-processing code:
	 * 			a. Label for `return` statements inside function body to jump to
	 * 			b. Restore registers
	 * 			c. Deallocate stack frame (set stack pointer to frame pointer)
	 * 			d. Reset fp to old location
	 * 			e. Return from function
	 */
	@Override
	protected CodeObject postprocess(FunctionNode node, CodeObject body) {
		CodeObject co = new CodeObject();
		
		//step 1
		String newfunlabel = generateFunctionLabel(node.getFuncName());
		Instruction funlabel = new Label(newfunlabel);
		co.code.add(funlabel);
		
		//step 2
		Instruction swoldfp = new Sw("fp", "sp", "0");
		co.code.add(swoldfp);
		Instruction mvfp2sp = new Mv("sp", "fp");
		co.code.add(mvfp2sp);
		Instruction updatesp = new Addi("sp", "-4", "sp");
		co.code.add(updatesp);
		
		//step 3
		String offset = "-" + Integer.toString(node.getScope().getNumLocals()*4); //might buggy
		Instruction newstackframe = new Addi("sp", offset, "sp");
		co.code.add(newstackframe);
		
		//step 4		

		for ( int i = 1; i < getIntRegCount() + 1; i=i+1 ) {
			Instruction swreg = new Sw( "t" + Integer.toString(i), "sp", "0" );
			co.code.add(swreg);
			Instruction regsonstack = new Addi("sp", "-4", "sp");
			co.code.add(regsonstack);
		}
		
		for ( int i = 1; i < getFloatRegCount() + 1; i=i+1 ) {
			Instruction swreg = new Fsw( "f" + Integer.toString(i), "sp", "0" );
			co.code.add(swreg);
			Instruction regsonstack = new Addi("sp", "-4", "sp");
			co.code.add(regsonstack);
		}
		
		//step 5
		co.code.addAll(body.code);
		
		//step 6a
		String returnlabel = generateFunctionOutLabel();
		Instruction retlabel = new Label(returnlabel);
		co.code.add(retlabel);
		
		//step 6b
		for ( int i = getFloatRegCount(); i > 0; i=i-1 ) {
			Instruction regsonstack = new Addi("sp", "4", "sp");
			co.code.add(regsonstack);
			Instruction lwreg = new Flw( "f" + Integer.toString(i), "sp", "0" );
			co.code.add(lwreg);
		}
		
		for ( int i = getIntRegCount(); i > 0; i=i-1 ) {
			Instruction regsonstack = new Addi("sp", "4", "sp");
			co.code.add(regsonstack);
			Instruction lwreg = new Lw( "t" + Integer.toString(i), "sp", "0" );
			co.code.add(lwreg);
		}
		
		//step 6c
		Instruction mvfp2old = new Mv("fp", "sp");
		co.code.add(mvfp2old);
		Instruction lwfp = new Lw("fp", "fp", "0");
		co.code.add(lwfp);
		
		//step 6d
		Instruction retfromfunc = new Ret();
		co.code.add(retfromfunc);
		
		co.temp = null;
		co.type = null;

		return co;
	}

	/**
	 * Generate code for the list of functions. This is the "top level" code generation function
	 * 
	 * Step 1: Set fp to point to sp
	 * 
	 * Step 2: Insert a JR to main
	 * 
	 * Step 3: Insert a HALT
	 * 
	 * Step 4: Include all the code of the functions
	 */
	@Override
	protected CodeObject postprocess(FunctionListNode node, List<CodeObject> funcs) {
		CodeObject co = new CodeObject();

		co.code.add(new Mv("sp", "fp"));
		co.code.add(new Jr(generateFunctionLabel("main")));
		co.code.add(new Halt());
		co.code.add(new Blank());

		//add code for each of the functions
		for (CodeObject c : funcs) {
			co.code.addAll(c.code);
			co.code.add(new Blank());
		}

		return co;
	}

	/**
	* 
	* FILL IN FOR STEP 4
	* 
	* Generate code for a call expression
	 * 
	 * Step 1: For each argument:
	 * 
	 * 	Step 1a: insert code of argument (don't forget to rvalify!)
	 * 
	 * 	Step 1b: push result of argument onto stack 
	 * 
	 * Step 2: alloate space for return value
	 * 
	 * Step 3: push current return address onto stack
	 * 
	 * Step 4: jump to function
	 * 
	 * Step 5: pop return address back from stack
	 * 
	 * Step 6: pop return value into fresh temporary (destination of call expression)
	 * 
	 * Step 7: remove arguments from stack (move sp)
	 * 
	 * Add special handling for malloc and free
	 */

	 /**
	  * FOR STEP 6: Make sure to handle VOID functions properly
	  */
	@Override
	protected CodeObject postprocess(CallNode node, List<CodeObject> args) {
		
		//STEP 0
		CodeObject co = new CodeObject();
		
		if (node.getArgs().size() != args.size()) {
			System.err.println("TYPE ERROR");
			System.exit(7);
		}
		else {
			int i = 0;
			for(CodeObject lco : args) {
				if (node.getArgs().get(i).getType() != lco.type) {
					System.err.println("TYPE ERROR");				
					System.exit(7);
				}
				i = i + 1;
			}
		}
		//step 1
		for(CodeObject lco : args) {
			//step 1a
			if(lco.lval) {
				lco = rvalify(lco);
				
			}
			co.code.addAll(lco.code);
			
			//step 1b
			switch (lco.getType().type) {
			case INT:
				Instruction swarg = new Sw( lco.temp, "sp", "0" ); 
				co.code.add(swarg);
				break;
			case FLOAT:
				swarg = new Fsw( lco.temp, "sp", "0" ); 
				co.code.add(swarg);
				break;
			case PTR:
				//System.err.println(lco.temp); 
				//System.err.println(lco.code.getLast().getDest());
				if(lco.temp != null) {
					swarg = new Sw( lco.temp, "sp", "0" ); 
					co.code.add(swarg);
				break;
				}
				else {
					swarg = new Sw( lco.code.getLast().getDest(), "sp", "0" );
					co.code.add(swarg);
				}
			case VOID:
				break;
			default:
				throw new Error("Wrong Call Type");
			}
			
			Instruction pushsp = new Addi("sp", "-4", "sp");
			co.code.add(pushsp);
		}
		
		//step 2
		Instruction returnsp = new Addi("sp", "-4", "sp");
		co.code.add(returnsp);
		
		//step 3
		Instruction pushret = new Sw("ra", "sp", "0");
		co.code.add(pushret);
		Instruction pushsp2 = new Addi("sp", "-4", "sp");
		co.code.add(pushsp2);
		
		//step 4
		String returnlabel = generateFunctionLabel(node.getFuncName());
		Instruction jumpreturnlabel = new Jr(returnlabel);
		co.code.add(jumpreturnlabel);

		
		//step 5
		Instruction pushsp3 = new Addi("sp", "4", "sp");
		co.code.add(pushsp3);
		
		Instruction popret2 = new Lw("ra",  "sp", "0");
		co.code.add(popret2);
		
		//step 6
		Instruction popsp2 = new Addi("sp", "4", "sp");
		co.code.add(popsp2);
		
		String newtemp = "0";
		switch (node.getType().type) {
		case INT:
			newtemp = generateTemp(node.getType().type);
			Instruction popret3 = new Lw(newtemp, "sp", "0");
			co.code.add(popret3);
			break;
		case FLOAT:
			newtemp = generateTemp(node.getType().type);
			popret3 = new Flw(newtemp, "sp", "0");
			co.code.add(popret3);
			break;
		case PTR:
			newtemp = generateTemp(node.getType().type);
			popret3 = new Lw(newtemp, "sp", "0");
			co.code.add(popret3);
			break;
		case VOID:
			break;
		default:
			throw new Error("Wrong Call Type");
		}
		
		//step 7
		int offset = 4*args.size();
		Instruction popsp3 = new Addi("sp", Integer.toString(offset), "sp");                         
		co.code.add(popsp3);
		
		co.temp = newtemp;
		co.lval = false;
		co.type = node.getType();		
		
		return co;
	}
	
	/**
	 * Generate code for * (expr)
	 * 
	 * Goal: convert the r-val coming from expr (a computed address) into an l-val (an address that can be loaded/stored)
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Rvalify expr if needed
	 * 
	 * Step 2: Copy code from expr (including any rvalification) into new code object
	 * 
	 * Step 3: New code object has same temporary as old code, but now is marked as an l-val
	 * 
	 * Step 4: New code object has an "unwrapped" type: if type of expr is * T, type of temporary is T. Can get this from node
	 */
	@Override
	protected CodeObject postprocess(PtrDerefNode node, CodeObject expr) {
		
		//step 0
		CodeObject co = new CodeObject();
		
		//step 1
		if (expr.lval) {
			expr = rvalify(expr);
			//System.err.println("Test");
		}
		
		//step 2
		co.code.addAll(expr.code);
		
		//step 3
		co.temp = expr.temp;
		co.lval = true;
		
		//step 4
		//if(expr.getType() != null) {
		//	co.type = expr.getType().getWrappedType();
		//}
		co.type = node.getType();
//		co.ste = expr.ste;

		return co;
	}

	/**
	 * Generate code for a & (expr)
	 * 
	 * Goal: convert the lval coming from expr (an address) to an r-val (a piece of data that can be used)
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: If lval is a variable, generate code to put address into a register (e.g., generateAddressFromVar)
	 *			Otherwise just copy code from other code object
	 * 
	 * Step 2: New code object has same temporary as existing code, but is an r-val
	 * 
	 * Step 3: New code object has a "wrapped" type. If type of expr is T, type of temporary is *T. Can get this from node
	 */
	@Override
	protected CodeObject postprocess(AddrOfNode node, CodeObject expr) {
		
		//step 0
		CodeObject co = new CodeObject();
		
		//step 1
		if(expr.getSTE() != null) {
			InstructionList il = generateAddrFromVariable(expr);
			co.temp = il.getLast().getDest();
			co.code.addAll(il);
		}
		co.code.addAll(expr.code);
		
		//step 2	
		co.lval = false;
		
		//step 3
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for malloc
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Add code from expression (rvalify if needed)
	 * 
	 * Step 2: Create new MALLOC instruction
	 * 
	 * Step 3: Set code object type to INFER
	 */
	@Override
	protected CodeObject postprocess(MallocNode node, CodeObject expr) {
		
		//step 0
		CodeObject co = new CodeObject();
		
		//step 1
		if(expr.lval) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		
		//step 2
		String newtemp = generateTemp(Scope.InnerType.INT);
		Instruction i = new Malloc(expr.temp, newtemp);
		co.code.add(i);
		
		//step 3
		co.type = node.getType();
		co.temp = newtemp;
		
		return co;
	}
	
	/**
	 * Generate code for free
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Add code from expression (rvalify if needed)
	 * 
	 * Step 2: Create new FREE instruction
	 */
	@Override
	protected CodeObject postprocess(FreeNode node, CodeObject expr) {
		
		//step 0
		CodeObject co = new CodeObject();
		
		//step 1
		if (expr.lval) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		
		//step 2
		Instruction i = new Free(expr.temp);
		co.code.add(i);
		
		return co;
	}

	/**
	 * Generate a fresh temporary
	 * 
	 * @return new temporary register name
	 */
	protected String generateTemp(Scope.InnerType t) {
		switch(t) {
			case INT: 
			case PTR: //works the same for pointers
				return intTempPrefix + String.valueOf(++intRegCount);
			case FLOAT: return floatTempPrefix + String.valueOf(++floatRegCount);
			default: throw new Error("Generating temp for bad type");
		}
	}
	
	protected String generateUnknownLabel() {
		return "unknown_" + String.valueOf(++unknownLabel);
	}
	protected String generateLoopLabel() {
		return "loop_" + String.valueOf(++loopLabel);
	}

	protected String generateElseLabel() {
		return  "else_" + String.valueOf(++elseLabel);
	}

	protected String generateOutLabel() {
		return "out_" +  String.valueOf(++outLabel);
	}

	protected String generateFunctionLabel() {
		return "func_" + currFunc;
	}

	protected String generateFunctionLabel(String func) {
		return "func_" + func;
	}

	protected String generateFunctionOutLabel() {
		return "func_ret_" + currFunc;
	}
	
	/**
	 * Take a code object that results in an lval, and create a new code
	 * object that adds a load to generate the rval.
	 * 
	 * @param lco The code object resulting in an address
	 * @return A code object with all the code of <code>lco</code> followed by a load
	 *         to generate an rval
	 */
	protected CodeObject rvalify(CodeObject lco) {
		
		//Step 0
		CodeObject co = new CodeObject();
		
		assert (lco.lval == true);
		
		//step 1
		SymbolTableEntry symbol = lco.getSTE();
		if(symbol!= null) {
			if (symbol.isLocal()) {				
				switch (lco.getType().type) {
				case INT:
					String newtemp = generateTemp(Scope.InnerType.INT);
					Instruction load = new Lw(newtemp, "fp", String.valueOf(lco.getSTE().addressToString()));
					co.code.add(load);
					co.code.addAll(lco.code);
					co.temp = newtemp;
					break;
				case FLOAT:
					newtemp = generateTemp(Scope.InnerType.FLOAT);
					load = new Flw(newtemp, "fp", String.valueOf(lco.getSTE().addressToString()));
					co.code.add(load);
					co.code.addAll(lco.code);
					co.temp = newtemp;
					break;
				case PTR:
					newtemp = generateTemp(Scope.InnerType.INT);
					
					//System.err.println(lco.getType());
					//System.err.println(lco.getType().getWrappedType());
					
					load = new Lw(newtemp, "fp", String.valueOf(lco.getSTE().addressToString()));
					co.code.add(load);
					co.code.addAll(lco.code);
					co.temp = newtemp;
					break;
				default: 
					throw new Error("Wrong type");
				}
			}
			else {
				InstructionList il = generateAddrFromVariable(lco);
				co.code.addAll(il);
				co.code.addAll(lco.code);
				switch(lco.getType().type) {
				case INT: 
					String newtemp = generateTemp(Scope.InnerType.INT);
					Instruction load = new Lw(newtemp, il.getLast().getDest(), "0");
					co.code.add(load);
					co.temp = newtemp;
					break;
				case FLOAT: newtemp = generateTemp(Scope.InnerType.FLOAT);
					load = new Flw(newtemp, il.getLast().getDest(), "0");
					co.code.add(load);
					co.temp = newtemp;
					break;
				case PTR:
					newtemp = generateTemp(Scope.InnerType.INT);
					
					//System.err.println(lco.getType());
					//System.err.println(lco.getType().getWrappedType());
					
					load = new Lw(newtemp, il.getLast().getDest(), "0");
					co.code.add(load);
					co.code.addAll(lco.code);
					co.temp = newtemp;
					break;
				default: 
					throw new Error("Wrong type");
				}
			}
		}
		else {
			//String newtemp = generateTemp(Scope.InnerType.INT);
			
			switch(lco.getType().type) {
			case INT: 
				String newtemp = generateTemp(Scope.InnerType.INT);
				Instruction load = new Lw(newtemp, lco.temp, "0");
				co.code.addAll(lco.code);
				co.code.add(load);
				co.temp = newtemp;
				break;
			case FLOAT: newtemp = generateTemp(Scope.InnerType.FLOAT);
				load = new Flw(newtemp, lco.temp, "0");
				co.code.addAll(lco.code);
				co.code.add(load);
				co.temp = newtemp;
				break;
			case PTR: 
				newtemp = generateTemp(Scope.InnerType.INT);
				load = new Lw(newtemp, lco.temp, "0");
				co.code.addAll(lco.code);
				co.code.add(load);
				co.temp = newtemp;
				break;
			default: 
				throw new Error("Wrong type");
			}
			
			
			//Instruction load = new Lw(newtemp, lco.temp, "0");
			//co.code.addAll(lco.code);
			//co.code.add(load);
			//co.temp = newtemp;
		}
		
		co.ste = lco.ste;
		co.type = lco.getType();
		co.lval = false;
		return co;
	}

	/**
	 * Generate an instruction sequence that holds the address of the variable in a code object
	 * 
	 * If it's a global variable, just get the address from the symbol table
	 * 
	 * If it's a local variable, compute the address relative to the frame pointer (fp)
	 * 
	 * @param lco The code object holding a variable
	 * @return a list of instructions that puts the address of the variable in a register
	 */
	private InstructionList generateAddrFromVariable(CodeObject lco) {

		InstructionList il = new InstructionList();

		//Step 1:
		SymbolTableEntry symbol = lco.getSTE();
		String address = symbol.addressToString();

		//Step 2:
		Instruction compAddr = null;
		if (symbol.isLocal()) {
			//If local, address is offset
			//need to load fp + offset
			//addi tmp' fp offset
			compAddr = new Addi("fp", address, generateTemp(Scope.InnerType.INT));
		} else {
			//If global, address in symbol table is the right location
			//la tmp' addr //Register type needs to be an int
			compAddr = new La(generateTemp(Scope.InnerType.INT), address);
		}
		il.add(compAddr); //add instruction to code object

		return il;
	}

}
