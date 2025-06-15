package edu.kit.kastel.vads.compiler.ir.node;

public final class ProjNode extends Node {
    public static final int IN = 0;
    private final ProjectionInfo projectionInfo;
    private ProjNode sibling;

    public ProjNode(Block block, Node in, ProjectionInfo projectionInfo) {
        super(block, in);
        this.projectionInfo = projectionInfo;
    }

    @Override
    protected String info() {
        return this.projectionInfo.toString();
    }

    public ProjectionInfo projectionInfo() {
        return projectionInfo;
    }

    public void setSibling(ProjNode sibling) {
        this.sibling = sibling;
    }

    public ProjNode getSibling() {
        return this.sibling;
    }

    public sealed interface ProjectionInfo {

    }

    public enum SimpleProjectionInfo implements ProjectionInfo {
        RESULT, SIDE_EFFECT
    }

    public enum BooleanProjectionInfo implements ProjectionInfo {
        TRUE(1), FALSE(0);

        public final int value;
        private BooleanProjectionInfo(int value) {
            this.value = value;
        }
    }
}
