//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package originem.utils.SRAMod.visual;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.magiclib.util.MagicAnim;

public class CustomBeamTrail extends BaseCombatLayeredRenderingPlugin {
    protected WeakReference<BeamAPI> beamRef;
    protected boolean shouldExpire;
    protected float nodeInterval = 10.0F;
    protected List<TrailNode> nodeList = new ArrayList();
    protected SpriteAPI textureSprite;
    protected float nodeRadiusMultiplier = 1.0F;
    protected float extraAlphaMult = 1.0F;
    protected float texPerNode = 0.05F;
    protected float texScrollSpeedMultiplier = 1.0F;
    protected float texCurOffset = 0.0F;
    protected int blendSrc = 770;
    protected int blendDst = 1;
    protected int nodeTransitionSplit = 3;
    protected int nodeTransitionMode = 0;
    protected Color overrideColor = null;

    public CustomBeamTrail(CombatEngineLayers layer, BeamAPI beam, SpriteAPI textureSprite) {
        super(layer);
        this.beamRef = new WeakReference(beam);
        this.textureSprite = textureSprite;
        Global.getCombatEngine().addLayeredRenderingPlugin(this);
    }

    public boolean isExpired() {
        return this.shouldExpire;
    }

    public void advance(float amount) {
        if (!this.shouldExpire) {
            if (this.checkShouldExpire()) {
                this.shouldExpire = true;
            } else {
                BeamAPI beam = (BeamAPI)this.beamRef.get();
                if (beam == null) {
                    this.shouldExpire = true;
                } else {
                    this.entity.getLocation().set(beam.getFrom());
                    float length = beam.getLengthPrevFrame();
                    int nodeCount = (int)(length / this.nodeInterval) + 2;
                    int nodeListSize = this.nodeList.size();
                    int i;
                    if (nodeCount > nodeListSize) {
                        for(i = 0; i < nodeCount - nodeListSize; ++i) {
                            this.nodeList.add(this.createTrailNode());
                        }
                    }

                    nodeListSize = this.nodeList.size();

                    for(i = 0; i < nodeListSize; ++i) {
                        TrailNode trailNode = (TrailNode)this.nodeList.get(i);
                        trailNode.elapsed += amount;
                        trailNode.advance(amount, i);
                    }

                    this.texCurOffset += this.texScrollSpeedMultiplier * amount;
                }
            }
        }
    }

    public boolean checkShouldExpire() {
        BeamAPI beam = (BeamAPI)this.beamRef.get();
        if (beam == null) {
            return true;
        } else if (beam.getWeapon() == null) {
            return true;
        } else if (!beam.getWeapon().isFiring()) {
            return true;
        } else if (beam.getBrightness() > 0.0F && beam.getWeapon().getChargeLevel() > 0.0F) {
            return !Global.getCombatEngine().getBeams().contains(beam);
        } else {
            return true;
        }
    }

    public TrailNode createTrailNode() {
        return new TrailNode();
    }

    public float getRenderRadius() {
        BeamAPI beam = (BeamAPI)this.beamRef.get();
        return beam == null ? 0.0F : beam.getLengthPrevFrame() * 1.2F;
    }

    public void setNodeRadiusMultiplier(float nodeRadiusMultiplier) {
        this.nodeRadiusMultiplier = nodeRadiusMultiplier;
    }

    public void setExtraAlphaMult(float extraAlphaMult) {
        this.extraAlphaMult = extraAlphaMult;
    }

    public void setOverrideColor(Color overrideColor) {
        this.overrideColor = overrideColor;
    }

    public void setNodeInterval(float nodeInterval) {
        this.nodeInterval = nodeInterval;
    }

    public void setTexPerNode(float texPerNode) {
        this.texPerNode = texPerNode;
    }

    public void setTexScrollSpeedMultiplier(float texScrollSpeedMultiplier) {
        this.texScrollSpeedMultiplier = texScrollSpeedMultiplier;
    }

    public void setBlendSrc(int blendSrc) {
        this.blendSrc = blendSrc;
    }

    public void setBlendDst(int blendDst) {
        this.blendDst = blendDst;
    }

    public void setNodeTransitionSplit(int nodeTransitionSplit) {
        this.nodeTransitionSplit = nodeTransitionSplit;
    }

    public void setNodeTransitionMode(int nodeTransitionMode) {
        this.nodeTransitionMode = nodeTransitionMode;
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (!this.shouldExpire) {
            int size = this.nodeList.size();
            if (size >= 2) {
                BeamAPI beam = (BeamAPI)this.beamRef.get();
                if (beam != null) {
                    boolean outlineMode = false;
                    GL11.glPushMatrix();
                    GL11.glEnable(3042);
                    GL11.glEnable(3553);
                    GL11.glBlendFunc(this.blendSrc, this.blendDst);
                    if (outlineMode) {
                        GL11.glDisable(3042);
                        GL11.glDisable(3553);
                        GL11.glPolygonMode(1032, 6913);
                    }

                    float angle = VectorUtils.getAngleStrict(beam.getFrom(), beam.getRayEndPrevFrame());
                    GL11.glTranslatef(beam.getFrom().x, beam.getFrom().y, 0.0F);
                    GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
                    this.textureSprite.bindTexture();
                    float curLength = beam.getLengthPrevFrame();
                    float totalAlphaMult = beam.getBrightness() * this.extraAlphaMult;
                    float lastNodeRadius = 0.0F;
                    float lastTex = this.texCurOffset;

                    for(int j = 0; j <= 1 && !((double)totalAlphaMult < 0.001); ++j) {
                        GL11.glBegin(5);

                        for(int i = 0; i < size; ++i) {
                            TrailNode node = (TrailNode)this.nodeList.get(i);
                            TrailNode nextNode;
                            if (i == size - 1) {
                                nextNode = node;
                            } else {
                                nextNode = (TrailNode)this.nodeList.get(i + 1);
                            }

                            boolean couldBreak = false;

                            for(int transitionIndex = 0; transitionIndex < this.nodeTransitionSplit; ++transitionIndex) {
                                float transitionFactor = (float)transitionIndex / (float)this.nodeTransitionSplit;
                                float nodeAtDistance = this.nodeInterval * ((float)i + transitionFactor);
                                float nodeTex = this.texPerNode * ((float)i + transitionFactor) + this.texCurOffset;
                                float nodeRadius;
                                if (this.nodeTransitionMode == 1) {
                                    nodeRadius = (node.radius + MagicAnim.smooth(transitionFactor) * (nextNode.radius - node.radius)) * beam.getBrightness();
                                } else {
                                    nodeRadius = (node.radius + transitionFactor * (nextNode.radius - node.radius)) * beam.getBrightness();
                                }

                                Color nodeColor;
                                if (this.overrideColor != null) {
                                    nodeColor = this.overrideColor;
                                } else {
                                    nodeColor = Misc.interpolateColor(node.color, nextNode.color, transitionFactor);
                                }

                                float nodeAlphaMult = 1.0F;
                                if (nodeAtDistance + 1.0F >= curLength) {
                                    float fadeFactor = (nodeAtDistance - curLength) / this.nodeInterval;
                                    nodeRadius += (lastNodeRadius - nodeRadius) * fadeFactor;
                                    nodeTex += (lastTex - nodeTex) * fadeFactor;
                                    nodeAtDistance = curLength;
                                    couldBreak = true;
                                }

                                if (nodeAtDistance == 0.0F) {
                                    nodeAlphaMult = 0.0F;
                                }

                                GL11.glColor4ub((byte)nodeColor.getRed(), (byte)nodeColor.getGreen(), (byte)nodeColor.getBlue(), (byte)((int)((float)nodeColor.getAlpha() * nodeAlphaMult * totalAlphaMult)));
                                GL11.glTexCoord2f(0.0F + (float)j * 0.5F, nodeTex);
                                GL11.glVertex2f(nodeAtDistance, nodeRadius * (1.0F - (float)j) * this.nodeRadiusMultiplier);
                                GL11.glColor4ub((byte)nodeColor.getRed(), (byte)nodeColor.getGreen(), (byte)nodeColor.getBlue(), (byte)((int)((float)nodeColor.getAlpha() * nodeAlphaMult * totalAlphaMult)));
                                GL11.glTexCoord2f(0.5F + (float)j * 0.5F, nodeTex);
                                GL11.glVertex2f(nodeAtDistance, nodeRadius * (float)(-j) * this.nodeRadiusMultiplier);
                                if (couldBreak) {
                                    GL11.glColor4ub((byte)nodeColor.getRed(), (byte)nodeColor.getGreen(), (byte)nodeColor.getBlue(), (byte)0);
                                    GL11.glTexCoord2f(0.0F + (float)j * 0.5F, nodeTex + 25.0F * this.texPerNode / this.nodeInterval);
                                    GL11.glVertex2f(nodeAtDistance + 25.0F, nodeRadius * (1.0F - (float)j) * this.nodeRadiusMultiplier);
                                    GL11.glColor4ub((byte)nodeColor.getRed(), (byte)nodeColor.getGreen(), (byte)nodeColor.getBlue(), (byte)0);
                                    GL11.glTexCoord2f(0.5F + (float)j * 0.5F, nodeTex + 25.0F * this.texPerNode / this.nodeInterval);
                                    GL11.glVertex2f(nodeAtDistance + 25.0F, nodeRadius * (float)(-j) * this.nodeRadiusMultiplier);
                                    break;
                                }

                                lastNodeRadius = nodeRadius;
                                lastTex = nodeTex;
                            }

                            if (couldBreak) {
                                break;
                            }
                        }

                        GL11.glEnd();
                    }

                    if (outlineMode) {
                        GL11.glPolygonMode(1032, 6914);
                    }

                    GL11.glPopMatrix();
                }
            }
        }
    }

    public static class TrailNode {
        public float radius = 100.0F;
        public float elapsed = MathUtils.getRandomNumberInRange(0.0F, 10.0F);
        public Color color;

        public TrailNode() {
            this.color = Color.white;
        }

        public void advance(float amount, int index) {
            this.radius = (float)(30.0 + Math.max(-5.0, 10.0 * FastTrig.sin((double)(this.elapsed * 2.0F))));
        }
    }
}
