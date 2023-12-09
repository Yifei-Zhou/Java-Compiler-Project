package assembly.instructions;

/**
 * Class corresponding to RISC-V FMOVI.S instruction
 * 
 * Models: FMOVI dest src #dest = src
 */

public class FmovI extends Instruction {
	
    /**
     * Initializes a FmovI instruction that will print: FMOVI.S dest src
     * 
     * @param src source operand 1
     * @param dest destination operand
     */
	
    public FmovI(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.FMOVI;
    }
	
    /**
     * @return "FMOVI.S dest src"
     */
    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }
}