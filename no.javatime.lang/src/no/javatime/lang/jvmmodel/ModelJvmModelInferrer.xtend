/*
 * generated by Xtext 2.9.1
 */
package no.javatime.lang.jvmmodel

import com.google.inject.Inject
import no.javatime.core.model.annotations.Action
import no.javatime.core.model.annotations.GetSeriesValue
import no.javatime.core.model.annotations.Insert
import no.javatime.core.model.annotations.SeriesValue
import no.javatime.core.model.annotations.SetSeriesValue
import no.javatime.core.model.annotations.Start
import no.javatime.core.model.annotations.StartValue
import no.javatime.core.model.annotations.Stop
import no.javatime.lang.model.Field
import no.javatime.lang.model.FieldTypeName
import no.javatime.lang.model.ModelElement
import no.javatime.lang.model.Operation
import no.javatime.lang.model.OperationTypeName
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.common.types.JvmAnnotationReference
import org.eclipse.xtext.common.types.JvmEnumerationType
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.common.types.JvmTypeReference
import org.eclipse.xtext.common.types.TypesFactory
import org.eclipse.xtext.common.types.util.TypeReferences
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotation
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder

/**
 * <p>Infers a JVM model from the source model.</p> 
 *
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
 */
class ModelJvmModelInferrer extends AbstractModelInferrer {

	/**
	 * convenience API to build and initialize JVM types and their members.
	 */
	@Inject extension JvmTypesBuilder 
	@Inject extension IQualifiedNameProvider 
	@Inject TypeReferences typeReferences 
	@Inject TypesFactory typesFactory

	def dispatch void infer(ModelElement modelElement, IJvmDeclaredTypeAcceptor acceptor, 
            boolean isPrelinkingPhase) {

    	acceptor.accept(modelElement.toClass(modelElement.fullyQualifiedName)) [
		
			fileHeader = 'Generated for the JavaTime Language by Xtext'			
			val annotationRef = annotationRef(typeof(no.javatime.core.model.annotations.ModelElement))			
			// val annotationRef = modelElement.toAnnotation(typeof(no.javatime.core.model.annotations.ModelElement))
			modelElement.addJvmEnumValueToAnnotation(annotationRef, "type", 
				typeof(no.javatime.core.model.annotations.ModelElement.Type), modelElement.typeName.toString.toUpperCase)
			annotations += annotationRef
	      	documentation = modelElement.documentation
			var EList<XAnnotation> modelElementAnnotations = modelElement.getModelElementAnnotations()
		  	addAnnotations(modelElementAnnotations)
	      	if (modelElement.superType != null) {
	        	superTypes += modelElement.superType.cloneWithProxies
	        }
	      	var EList<JvmTypeReference> implements = modelElement.getImplements()
	      	for (implement : implements){
	      		superTypes += implement.cloneWithProxies
	      	}	      
	      	for (member : modelElement.members) {	        
	        	switch member {
	          	Field : {
	       			it.members += member.toField(member.name, member.type) [  
		        		documentation = member.documentation
						createAnnotation(annotations, member.typeName)											
						var EList<XAnnotation> fieldAnnotations = member.getFieldAnnotations()	      	
						addAnnotations(fieldAnnotations)
						initializer = member.initExp
		        	]
		           		val String operationName = member.name.toFirstUpper
		           		it.members += member.toMethod('get'+ operationName, member.type) [
							if (member.typeName.equals(FieldTypeName.SERIESVALUE)) {
								documentation = getSeriesGetterDoc()
								createAnnotation(annotations, OperationTypeName.GETSERIESVALUE)													        		
							} else {
								documentation = getGetterDoc(member.name);
							}
		        			body = ''' return this.�member.name�;'''
		        		]
		           		it.members += member.toMethod('set'+ operationName, typeRef(Void.TYPE)) [
							if (member.typeName.equals(FieldTypeName.SERIESVALUE)) {
								documentation = getSeriesSetterDoc(member.name)
								createAnnotation(annotations, OperationTypeName.SETSERIESVALUE)					
							} else {
								documentation = getSetterDoc(member.name);
							}
		               		parameters += member.toParameter(member.name, member.type)
		        			body = ''' this.�member.name� = �member.name�;'''
		        			
		        		]		        		
	          	}      
		        Operation : {
		        	it.members += member.toMethod(member.name, member.type) [
			       		documentation = member.documentation			       		
						createAnnotation(annotations, member.typeName)					
						var EList<XAnnotation> methodAnnotations = member.getMethodAnnotations()
						addAnnotations(methodAnnotations)	      	
				   		var EList<JvmFormalParameter> formalParameters = member.getFormalParameters()
		          		for (formalParameter : formalParameters) {
		            		parameters += formalParameter.toParameter(formalParameter.name, formalParameter.parameterType)
		           		}
	              		body = member.body
		           	]
	          	}
	        }
	      }
	    ]
	}

	def void createAnnotation(EList<JvmAnnotationReference> annotations, FieldTypeName fieldType) {

		var Class<?> annotationType = null
		
		switch (fieldType) {
			case FieldTypeName.SERIESVALUE: {
				annotationType = typeof(SeriesValue)				
			}
			case FieldTypeName.INSERT: {
				annotationType = typeof(Insert)				
			}
			default: {
			}
		}
		if (null != annotationType) {
			annotations += annotationRef(annotationType);
		}
	}
	
	def void createAnnotation(EList<JvmAnnotationReference> annotations, OperationTypeName operationType) {

		var Class<?> annotationType = null
		
		switch (operationType) {
			case OperationTypeName.START: {
				annotationType = typeof(Start)				
			}
			case OperationTypeName.STOP: {
				annotationType = typeof(Stop)				
			}
			case OperationTypeName.SETSERIESVALUE: {
				annotationType = typeof(SetSeriesValue)				
			}
			case OperationTypeName.GETSERIESVALUE: {
				annotationType = typeof(GetSeriesValue)				
			}
			case OperationTypeName.STARTVALUE: {
				annotationType = typeof(StartValue)				
			}
			case OperationTypeName.ACTION: {
				annotationType = typeof(Action)				
			}
			default: {
			}
		}
		if (null != annotationType) {
			annotations += annotationRef(annotationType);
		}
	}
	
	def void addJvmEnumValueToAnnotation(EObject sourceElement, JvmAnnotationReference annotationReference,
		String valueName, Class<?> enumerationClass, String literalName){
		
		val iterOp = annotationReference.annotation.getDeclaredOperations().filter(o|o.simpleName == valueName).head
		val annotationVal  = TypesFactory::eINSTANCE.createJvmEnumAnnotationValue
		annotationVal.operation = iterOp
		val enumType = typeReferences.getTypeForName(enumerationClass, sourceElement).type as JvmEnumerationType
		val literal = enumType.literals.filter(o|o.simpleName == literalName).head
		annotationVal.values+=literal
		annotationReference.explicitValues+=annotationVal
	}
	
	def JvmAnnotationReference addJvmStringValueAnnotation(EObject ctx, String typeName, Pair<String,String>... values) {
		
		val annotationRef = annotationRef(typeName)
		for (v : values) {
			val t = typesFactory.createJvmStringAnnotationValue
			t.values.add(v.value)
			t.operation = annotationRef.annotation.members.filter(JvmOperation).filter[simpleName==v.key].head
			annotationRef.explicitValues += t
		}
		annotationRef
	}
	
	def String getSeriesGetterDoc() {
'''Get the calculated time series value from the current or the 
previous step
<p> 
The simulator updates the time series value after it is returned by the
method in this class annotated with @�Action.simpleName�. Before the method
returns, it is the value from the previous step and after the method 
returns, it is the value from the current step.

@return The last calculated time series value'''
		
	}
	
	def String getGetterDoc(String fieldName) {
'''Get the �fieldName�

@return The �fieldName�'''
		
	}
	
	def String getSetterDoc(String parameterName) {
'''Set the �parameterName�

@param �parameterName� The �parameterName� to set'''
	}

	def String getSeriesSetterDoc(String parameterName) {
'''Set the calculated time series value of the current or the previous step
<p> 
The series value of the current step is set by the simulator after the 
value of the current step is calculated and returned by the method in this
class annotated with @�Action.simpleName�
<p>
If the series value is set before executing the method annotated 
with @�Action.simpleName�, it implies modifying the time series value
calculated in the previous step and if set afterwards, it implies
modifying the time series value at the current step.

@param �parameterName� Time series value of the current or 
the previous step to set'''
	}
}
