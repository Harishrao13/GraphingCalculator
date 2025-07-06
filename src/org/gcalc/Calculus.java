package org.gcalc;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import java.util.ArrayList;
import java.util.List;

public class Calculus {
    private static final ExprEvaluator evaluator = new ExprEvaluator();

    public static String differentiate(String equation) {
        try {
            String function = extractFunction(equation);
            IExpr derivative = evaluator.eval("D(" + function + ",x)");
            return derivative.toString().toLowerCase();
        } catch (Exception e) {
            return "Error in differentiation: " + e.getMessage();
        }
    }

    public static String findCriticalPoints(String equation) {
        try {
            String function = extractFunction(equation);
            IExpr firstDeriv = evaluator.eval("D(" + function + ",x)");
            IExpr secondDeriv = evaluator.eval("D(" + firstDeriv.toString() + ",x)");

            // Find only REAL critical points
            IExpr criticalPoints = evaluator
                    .eval("Select[NSolve[" + firstDeriv.toString() + "==0,x], Element[x/.#,Reals]&]");

            StringBuilder analysis = new StringBuilder();
            analysis.append("Critical Points Analysis (Real):\n");

            String pointsStr = criticalPoints.toString();
            if (pointsStr.equals("{}")) {
                analysis.append("No real critical points found");
            } else {
                pointsStr = pointsStr.replaceAll("[{}]", "");
                String[] solutions = pointsStr.split(",");

                for (String solution : solutions) {
                    if (!solution.contains("x->"))
                        continue;

                    try {
                        String xExpr = solution.split("x->")[1].trim();
                        double xValue = evaluator.eval("N(" + xExpr + ")").evalDouble();
                        double secondDerivValue = evaluator.eval("N(" + secondDeriv + "/.x->" + xExpr + ")")
                                .evalDouble();
                        double yValue = evaluator.eval("N(" + function + "/.x->" + xExpr + ")").evalDouble();

                        analysis.append(String.format("\nAt x ≈ %.3f (y ≈ %.3f): ", xValue, yValue));

                        if (secondDerivValue > 0) {
                            analysis.append("Local minimum");
                        } else if (secondDerivValue < 0) {
                            analysis.append("Local maximum");
                        } else {
                            analysis.append("Possible inflection point");
                        }
                    } catch (Exception e) {
                        analysis.append("\nCould not analyze point: ").append(solution);
                    }
                }
            }

            return analysis.toString();
        } catch (Exception e) {
            return "Error in analysis: " + e.getMessage();
        }
    }

    public static String integrate(String equation) {
    try {
        Equation eq = new Equation(equation);
        String function = extractFunction(eq.getRawEquation());

        if (eq.hasIntegralLimits()) {
            String integralExpr = "N[Integrate[" + function + ", {x, " + 
                                eq.getIntegralLowerLimit() + ", " + 
                                eq.getIntegralUpperLimit() + "}]]";
            IExpr result = evaluator.eval(integralExpr);
            return "∫ from " + eq.getIntegralLowerLimit() + " to " + 
                   eq.getIntegralUpperLimit() + " " + function + " dx = " + 
                   String.format("%.4f", result.evalDouble());
        } else {
            // Indefinite integral
            IExpr integral = evaluator.eval("Integrate[" + function + ", x]");
            return "∫ " + function + " dx = " + integral.toString();
        }
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
    }

    private static String extractFunction(String equation) {
        return equation.contains("=") ? equation.split("=")[1].trim() : equation.trim();
    }

    private static List<IExpr> extractSolutions(IExpr solutions) {
        List<IExpr> result = new ArrayList<>();
        String solStr = solutions.toString();

        if (solStr.startsWith("{{x->")) {
            // Single solution
            result.add(evaluator.eval(solStr.substring(4, solStr.length() - 2)));
        } else if (solStr.contains("},")) {
            // Multiple solutions
            String[] parts = solStr.split("},");
            for (String part : parts) {
                int start = part.indexOf("x->") + 3;
                int end = part.indexOf("}");
                if (start >= 3 && end > start) {
                    result.add(evaluator.eval(part.substring(start, end)));
                }
            }
        }
        return result;
    }

    // New method to get detailed analysis
    public static String getFunctionAnalysis(String equation) {
        try {
            String function = extractFunction(equation);
            IExpr firstDeriv = evaluator.eval("D(" + function + ",x)");
            IExpr secondDeriv = evaluator.eval("D(" + firstDeriv.toString() + ",x)");

            StringBuilder analysis = new StringBuilder();
            analysis.append("Function Analysis for: ").append(function).append("\n\n");

            // 1. Critical points and nature
            analysis.append(findCriticalPoints(equation)).append("\n");

            // 2. Concavity analysis
            analysis.append("Concavity Analysis:\n");
            IExpr inflectionPoints = evaluator.eval("Solve(" + secondDeriv.toString() + "==0,x)");
            List<IExpr> inflectionSolutions = extractSolutions(inflectionPoints);

            if (inflectionSolutions.isEmpty()) {
                analysis.append("No inflection points - constant concavity\n");
            } else {
                for (IExpr point : inflectionSolutions) {
                    double x = evaluator.eval("N(" + point + ")").evalDouble();
                    double leftValue = evaluator.eval("N(" + secondDeriv + "/.x->" + (x - 0.1) + ")").evalDouble();
                    double rightValue = evaluator.eval("N(" + secondDeriv + "/.x->" + (x + 0.1) + ")").evalDouble();

                    analysis.append("At x = ").append(point).append(":\n");
                    analysis.append("  Left concavity: ").append(leftValue > 0 ? "Upwards" : "Downwards").append("\n");
                    analysis.append("  Right concavity: ").append(rightValue > 0 ? "Upwards" : "Downwards")
                            .append("\n");
                }
            }

            return analysis.toString();
        } catch (Exception e) {
            return "Error in function analysis: " + e.getMessage();
        }
    }
}