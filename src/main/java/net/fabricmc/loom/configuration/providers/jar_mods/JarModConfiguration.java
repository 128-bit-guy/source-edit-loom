package net.fabricmc.loom.configuration.providers.jar_mods;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.fmj.ModEnvironment;

import org.apache.commons.codec.digest.DigestUtils;

public class JarModConfiguration {
	public List<JarMod> jarMods;

	private JarModConfiguration(List<JarMod> jarMods) {
		this.jarMods = jarMods;
	}

	public static JarModConfiguration create(ConfigContext context) {
		Project project = context.project();
		Configuration common = project.getConfigurations()
				.getByName(Constants.Configurations.MINECRAFT_JAR_MODS);
		Configuration client = project.getConfigurations()
				.getByName(Constants.Configurations.MINECRAFT_CLIENT_JAR_MODS);
		Configuration server = project.getConfigurations()
				.getByName(Constants.Configurations.MINECRAFT_SERVER_JAR_MODS);
		List<DependencyInfo> commonDependencies = getDependencies(project, common);
		List<DependencyInfo> clientDependencies = getDependencies(project, client);
		List<DependencyInfo> serverDependencies = getDependencies(project, server);
		List<JarMod> jarMods = new ArrayList<>();
		jarMods.addAll(getJarMods(commonDependencies, ModEnvironment.UNIVERSAL));
		jarMods.addAll(getJarMods(clientDependencies, ModEnvironment.CLIENT));
		jarMods.addAll(getJarMods(serverDependencies, ModEnvironment.SERVER));
		return new JarModConfiguration(jarMods);
	}

	private static List<DependencyInfo> getDependencies(Project project, Configuration configuration) {
		List<DependencyInfo> dependencies = new ArrayList<>();
		for (Dependency dependency : configuration.getDependencies()) {
			dependencies.add(DependencyInfo.create(project, dependency, configuration));
		}
		return dependencies;
	}

	private static List<JarMod> getJarMods(List<DependencyInfo> dependencies, ModEnvironment environment) {
		List<JarMod> jarMods = new ArrayList<>();
		for (DependencyInfo dependency : dependencies) {
			jarMods.add(new JarMod(dependency.getDepString(), dependency.resolveFile().get().toPath(), environment));
		}
		return jarMods;
	}

	public String getJarModNameExtension() {
		StringBuilder result = new StringBuilder();
		for (JarMod jarMod : jarMods) {
			result.append("-").append(jarMod.environment().name);
			result.append("-").append(jarMod.name().replace(':', '.'));
		}
		return "-" + DigestUtils.sha256Hex(result.toString());
	}
}
