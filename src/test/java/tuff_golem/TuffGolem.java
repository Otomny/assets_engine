package tuff_golem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.ModelConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TuffGolem extends GenericModelImpl {
    @Override
    public String getId() {
        return "tuff_golem.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, LivingEntity masterEntity) {
        super.init(instance, position, ModelConfig.defaultConfig, masterEntity);
    }
}
