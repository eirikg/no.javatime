package no.javatime.lang.design.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;

import no.javatime.lang.model.ModelElementType;

public class ModelServices {
    
	public EObject openTextEditor(EObject any) {
		System.out.println("Double click invoking openTextEditor: "  + any);
		return any;
	}
	static int st = 0;
	public Collection<EObject> getModelElementTypes(EObject any) {

		List<EObject> result = new ArrayList<>();		
		
		
		System.out.println(++st + " " + any);
		System.out.println(st + " eContainer(): " + any.eContainer());		
		System.out.println(st + " eClass(): " + any.eClass());		
		// empty System.out.println(st + " eAllContents(): " + any.eAllContents());		
		System.out.println(st + " eContainmentFeature(): " + any.eContainmentFeature());		
		System.out.println(st + " eContents(): " + any.eContents());		

		List<EObject> contents = any.eContents();
		for (EObject eObject : contents) {
			System.out.println(st + " eObject: " + eObject);						
			if (eObject.getClass().getName().compareTo("no.javatime.lang.model.impl.FieldImpl") == 0) {
				// Field field = (Field) eObject;
				System.out.println(st + " Field eContents: " + eObject.eContents());						
				List<EObject> jvmTypeRefs = eObject.eContents();
				for (EObject jvmTypeRef : jvmTypeRefs) {
					System.out.println(st + " Field jvmTypeRef: " + jvmTypeRef.getClass().toString());	
					JvmParameterizedTypeReference j = (JvmParameterizedTypeReference) jvmTypeRef;
					JvmType jvmType = j.getType();
					System.out.println(st + " jvm type " + jvmType.getQualifiedName());	

					System.out.println("packageElements" + any.eContainer().eContents());
					// PackageDeclaration pd = (PackageDeclaration) any.eContainer();
					// EList<Element> elems = pd.getPackageElements();
					for(EObject elem : any.eContainer().eContents()) {
						System.out.println(st +  " elem " + elem.getClass().getName() + " == " + jvmType.getQualifiedName());
						if (jvmType.getQualifiedName().endsWith(((ModelElementType) elem).getName())) {
							result.add(elem);							
						}
					}
//					System.out.println(st + " eReference " + pd.getName());	
					
				}
			}
		}
//		if (any instanceof ModelElementType) {
//			ModelElementType elemType = (ModelElementType) any;
//			System.out.println(st + " " + elemType);
//			EList<Member> members = elemType.getMembers();
//			for (Member member : members) {
//				System.out.println(st + " Member " + member);
//				if (member instanceof Field) {
//					Field fieldMember = (Field) member;
//					JvmTypeReference typeRef = fieldMember.getMemberType();
//					System.out.println(st + " JvmTypeReference " + typeRef);
//				}
//			}
//		}
		return result;
			
//		Family family = (Family) person.eContainer();
//        List<Person> result = new ArrayList<Person>();
//        for (Person person2 : family.getMembers()) {
//            if (person2.getMother() == null)
//                result.add(person2);
//        }
//        return result;
	}
	
	public boolean isValid(EObject any) {
		System.out.println(st++ + " Valid: " + any);		
		return true;
	}
}
