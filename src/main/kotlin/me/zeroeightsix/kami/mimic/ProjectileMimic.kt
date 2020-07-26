package me.zeroeightsix.kami.mimic

import me.zeroeightsix.kami.util.EntityUtil
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RayTraceContext
import net.minecraft.world.World

class ProjectileMimic(val world: World, shooter: LivingEntity, val type: EntityType<*>, val drag: Double) :
    TrajectoryMimic {

    override var x = 0.0
    override var y = 0.0
    override var z = 0.0
    override var landed = false
    override var entity: Entity? = null
    override var block: BlockPos? = null

    val dimensions = type.dimensions
    private lateinit var velocity: Vec3d
    lateinit var boundingBox: Box
    var yaw = 0f
    var pitch = 0f
    var prevYaw = 0f
    var prevPitch = 0f

    init {
        val pos = EntityUtil.getInterpolatedPos(shooter, MinecraftClient.getInstance().tickDelta)
        setPosition(pos.x, pos.y + shooter.standingEyeHeight - 0.10000000149011612, pos.z)
    }

    override fun tick() {
        var vec3d = velocity

        if (this.prevPitch == 0.0f && this.prevYaw == 0.0f) {
            val f = MathHelper.sqrt(Entity.squaredHorizontalLength(vec3d))
            this.yaw = (MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875).toFloat()
            this.pitch = (MathHelper.atan2(vec3d.y, f.toDouble()) * 57.2957763671875).toFloat()
            this.prevYaw = this.yaw
            this.prevPitch = this.pitch
        }

        val blockPos = BlockPos(this.x, this.y, this.z)
        val blockState: BlockState = this.world.getBlockState(blockPos)
        if (!blockState.isAir) {
            val voxelShape = blockState.getCollisionShape(this.world, blockPos)
            if (!voxelShape.isEmpty) {
                val var6: Iterator<*> = voxelShape.boundingBoxes.iterator()
                while (var6.hasNext()) {
                    val box = var6.next() as Box
                    if (box.offset(blockPos).contains(Vec3d(this.x, this.y, this.z))) {
                        this.landed = true
                        return
                    }
                }
            }
        }

        val vec3d2 = Vec3d(x, y, z)
        var vec3d3 = vec3d2.add(vec3d)
        val hitResult: HitResult = world.rayTrace(
            RayTraceContext(
                vec3d2,
                vec3d3,
                RayTraceContext.ShapeType.COLLIDER,
                RayTraceContext.FluidHandling.NONE,
                MinecraftClient.getInstance().player
            )
        )
        if (hitResult.type != HitResult.Type.MISS) {
            vec3d3 = hitResult.pos
        }

        vec3d = velocity
        val d = vec3d.x
        val e = vec3d.y
        val g = vec3d.z

        x += d
        y += e
        z += g
        val h = MathHelper.sqrt(Entity.squaredHorizontalLength(vec3d))
        yaw = (MathHelper.atan2(d, g) * 57.2957763671875).toFloat()

        pitch = (MathHelper.atan2(e, h.toDouble()) * 57.2957763671875).toFloat()
        while (pitch - prevPitch < -180.0f) {
            prevPitch -= 360.0f
        }
        while (pitch - prevPitch >= 180.0f) {
            prevPitch += 360.0f
        }
        while (yaw - prevYaw < -180.0f) {
            prevYaw -= 360.0f
        }
        while (yaw - prevYaw >= 180.0f) {
            prevYaw += 360.0f
        }
        pitch = MathHelper.lerp(0.2f, prevPitch, pitch)
        yaw = MathHelper.lerp(0.2f, prevYaw, yaw)
        val slowdown = if (isInWater(boundingBox)) {
            drag
        } else {
            0.99
        }

        velocity = vec3d.multiply(slowdown)
        velocity = Vec3d(velocity.x, velocity.y - 0.05000000074505806, velocity.z)

        setPosition(x, y, z)

        landed = landed || y < 0
    }

    fun setProperties(
        user: Entity,
        pitch: Float,
        yaw: Float,
        speed: Float
    ) {
        val i = -MathHelper.sin(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)
        val j = -MathHelper.sin(pitch * 0.017453292f)
        val k = MathHelper.cos(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)

        setVelocity(i.toDouble(), j.toDouble(), k.toDouble(), speed)

        this.setVelocity(i.toDouble(), j.toDouble(), k.toDouble(), speed)
        velocity = velocity.add(user.velocity.x, if (user.onGround) 0.0 else user.velocity.y, user.velocity.z)
    }

    fun setPosition(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
        val f: Float = this.dimensions.width / 2.0f
        val g: Float = this.dimensions.height
        boundingBox = Box(x - f.toDouble(), y, z - f.toDouble(), x + f.toDouble(), y + g.toDouble(), z + f.toDouble())
    }

    private fun setVelocity(
        x: Double,
        y: Double,
        z: Double,
        speed: Float
    ) {
        val vec3d = Vec3d(x, y, z).normalize().multiply(speed.toDouble())
        velocity = vec3d
        val f = MathHelper.sqrt(Entity.squaredHorizontalLength(vec3d))
        yaw = (MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875).toFloat()
        pitch = (MathHelper.atan2(vec3d.y, f.toDouble()) * 57.2957763671875).toFloat()
        prevYaw = yaw
        prevPitch = pitch
    }

}
