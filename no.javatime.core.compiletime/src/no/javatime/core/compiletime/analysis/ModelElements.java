package no.javatime.core.compiletime.analysis;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Introspector;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;

public class ModelElements {

	public static Collection<Class<?>> getModelClasses(Bundle bundle) {
		BundleRegion bundleRegion = Extenders.getService(BundleRegion.class);
		BundleProjectCandidates bundleProject = Extenders.getService(BundleProjectCandidates.class);
		IProject project = bundleRegion.getProject(bundle);
		if (null == project) {
			return null;
		}	
		IJavaProject javaProject = bundleProject.getJavaProject(project);
		return getModelClasses(javaProject, bundle);
	}
	
	/**
	 * Collects all active classes in the given JavaTime project.
	 * 
	 * @param project a given project with JavaTime nature
	 * @return all active classes in the given JavaTime project or an empty collection
	 */
	public static Collection<Class<?>> getModelClasses(IJavaProject javaProject, Bundle bundle) {

		Collection<Class<?>> cl = new LinkedList<Class<?>>();
		try {
			for (IPackageFragment pf : getSourcePackages(javaProject)) {
				for (ICompilationUnit cu : getCompilationUnits(pf)) {
					String qfn = pf.getElementName() + "." + 
						cu.getElementName().substring(0, cu.getElementName().length()-5);
					if (isModelMemberAnnot(cu)) {
						// Class<?>	c = Class.forName(qfn);
						Class<?>	c = Introspector.loadClass(bundle, qfn);
						cl.add(c);						
					}	
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return cl;
		} catch (ExtenderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cl;
	}
	
	private static Collection<IPackageFragment> getSourcePackages(IJavaProject javaProject) throws JavaModelException {

		Collection<IPackageFragment> pfl = new LinkedList<>();
		
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for (IPackageFragment p : packages) {
			// Source packages only
			if (p.getKind() == IPackageFragmentRoot.K_SOURCE) {
				pfl.add(p);
			}
		}
		return pfl;
	}
	
	private static Collection<ICompilationUnit> getCompilationUnits(IPackageFragment sourcepackage)
			throws JavaModelException {
		Collection<ICompilationUnit> cul = new LinkedList<ICompilationUnit>();
		
		for (ICompilationUnit cu : sourcepackage.getCompilationUnits()) {
			//System.out.println("Source file " + cu.getElementName());
			if (isModelMemberAnnot(cu)) {
				cul.add(cu);
			}
		}
		return cul;
	}

	private static boolean isModelMemberAnnot(ICompilationUnit unit) throws JavaModelException {

		IType[] types = unit.getTypes();
		for (IType type : types) {
			for (IAnnotation a : type.getAnnotations()) {
				if (a instanceof ModelElement) {
						return true;
				}
			}
		}
		return false;
	}

}
