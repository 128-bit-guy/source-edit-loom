package net.fabricmc.loom.configuration.providers.mappings.default_package;

import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingLayer;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record DefaultPackageLayerImpl (Map<String, String> renamedClasses) implements MappingLayer {
	private static final String DST_NS = MappingsNamespace.NAMED.toString();
	@Override
	public void visit(MappingVisitor visitor) throws IOException {
		if(visitor instanceof MappingTree mappings) {
			System.out.println(mappings.getDstNamespaces());
			int nsid = mappings.getNamespaceId(DST_NS);
			if (visitor.visitHeader()) {
				visitor.visitNamespaces(mappings.getSrcNamespace(), mappings.getDstNamespaces());
			}
			if (visitor.visitContent()) {
				for(Map.Entry<String, String> renamedClass : renamedClasses.entrySet()) {
					String className = renamedClass.getKey();

					if (visitor.visitClass(className)) {
						visitor.visitDstName(MappedElementKind.CLASS, nsid, renamedClass.getValue());
					}
				}
			}

			visitor.visitEnd();
		}
	}

	@Override
	public MappingsNamespace getSourceNamespace() {
		return MappingsNamespace.INTERMEDIARY;
	}

	@Override
	public List<Class<? extends MappingLayer>> dependsOn() {
		return Collections.singletonList(IntermediaryMappingLayer.class);
	}
}
