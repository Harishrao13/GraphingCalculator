package org.gcalc;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import java.util.HashMap;
import java.util.Map;

public class Calculus {
    private static final ExprEvaluator evaluator = new ExprEvaluator();
    
    public static String differentiate(String equation) {
        try {
            // Extract function if equation is in form "y=..."
            String function = equation.contains("=") 
                ? equation.split("=")[1].trim() 
                : equation.trim();
            
            IExpr derivative = evaluator.eval("D(" + function + ",x)");
            return derivative.toString();
        } catch (Exception e) {
            return "Error in differentiation: " + e.getMessage();
        }
    }
    
    public static String findCriticalPoints(String equation) {
        try {
            String function = equation.contains("=") 
                ? equation.split("=")[1].trim() 
                : equation.trim();
            
            // Get first derivative
            IExpr firstDeriv = evaluator.eval("D(" + function + ",x)");
            
            // Find where f'(x) = 0
            IExpr criticalPoints = evaluator.eval("Solve(" + firstDeriv.toString() + "==0,x)");
            
            // Get second derivative for classification
            IExpr secondDeriv = evaluator.eval("D(" + firstDeriv.toString() + ",x)");
            
            // Analyze each critical point
            StringBuilder analysis = new StringBuilder();
            analysis.append("Critical Points: ").append(criticalPoints).append("\n");
            
            // Here you would add logic to evaluate second derivative at each point
            // to classify as maxima/minima/inflection
            
            return analysis.toString();
        } catch (Exception e) {
            return "Error in finding critical points: " + e.getMessage();
        }
    }
    
    public static String integrate(String equation) {
        try {
            String function = equation.contains("=") 
                ? equation.split("=")[1].trim() 
                : equation.trim();
            
            IExpr integral = evaluator.eval("Integrate(" + function + ",x)");
            return integral.toString();
        } catch (Exception e) {
            return "Error in integration: " + e.getMessage();
        }
    }
}