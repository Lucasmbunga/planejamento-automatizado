import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StripsPlanner {

    record GroundAction(
            String name,
            Set<String> preconditions,
            Set<String> addEffects,
            Set<String> deleteEffects
    ) {
        boolean isApplicable(Set<String> state) {
            return state.containsAll(preconditions);
        }

        Set<String> apply(Set<String> state) {
            Set<String> nextState = new HashSet<>(state);
            nextState.removeAll(deleteEffects);
            nextState.addAll(addEffects);
            return Set.copyOf(nextState);
        }
    }

    record SearchNode(Set<String> state, List<String> plan) {}

    static int generatedStates = 0;
    static int expandedStates = 0;

    static final class ActionSchema {
        String name;
        List<String> parameters = new ArrayList<>();
        List<List<String>> preconditions = new ArrayList<>();
        List<List<String>> addEffects = new ArrayList<>();
        List<List<String>> deleteEffects = new ArrayList<>();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java StripsPlanner domain.pddl problem.pddl");
            System.exit(1);
        }

        try {
            List<Object> domain = parse(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
            List<Object> problem = parse(Files.readString(Path.of(args[1]), StandardCharsets.UTF_8));

            String declaredDomain = getDomainName(problem);

            if (!declaredDomain.equalsIgnoreCase("blocksworld")) {
                System.err.println("Aviso: o problema declara o domínio '" + declaredDomain
                        + "', mas este planejador foi preparado para 'blocksworld'.");
            }

            List<String> objects = getObjects(problem);
            Set<String> initialState = getInitialState(problem);
            Set<String> goal = getGoal(problem);

            List<ActionSchema> schemas = getActionSchemas(domain);
            List<GroundAction> actions = instantiateActions(schemas, objects);

            System.out.println("=== Planejador STRIPS + BFS ===");
            System.out.println("Domínio: " + declaredDomain);
            System.out.println("Objetos: " + objects);
            System.out.println("Estado inicial: " + initialState);
            System.out.println("Meta: " + goal);
            System.out.println("Ações instanciadas: " + actions.size());
            System.out.println();

            long startTime = System.nanoTime();
            Optional<List<String>> result = solve(initialState, goal, actions);
            long elapsedTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            if (result.isPresent()) {
                List<String> plan = result.get();

                System.out.println("Plano encontrado:");
                for (int i = 0; i < plan.size(); i++) {
                    System.out.printf("%d. %s%n", i + 1, plan.get(i));
                }

                System.out.println();
                System.out.println("Comprimento do plano: " + plan.size());
            } else {
                System.out.println("Nenhuma solução foi encontrada.");
            }

            System.out.println("Estados gerados: " + generatedStates);
            System.out.println("Estados expandidos: " + expandedStates);
            System.out.println("Tempo de execução: " + elapsedTimeMs + " ms");

        } catch (IOException e) {
            System.err.println("Erro ao ler ficheiro: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro durante o planejamento: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static Optional<List<String>> solve(
            Set<String> initialState,
            Set<String> goal,
            List<GroundAction> actions
    ) {
        Queue<SearchNode> frontier = new ArrayDeque<>();
        Set<Set<String>> visited = new HashSet<>();

        Set<String> startState = Set.copyOf(initialState);
        frontier.add(new SearchNode(startState, List.of()));
        visited.add(startState);

        while (!frontier.isEmpty()) {
            SearchNode currentNode = frontier.remove();
            expandedStates++;

            if (currentNode.state().containsAll(goal)) {
                return Optional.of(currentNode.plan());
            }

            for (GroundAction action : actions) {
                if (!action.isApplicable(currentNode.state())) {
                    continue;
                }

                Set<String> nextState = action.apply(currentNode.state());

                if (visited.add(nextState)) {
                    generatedStates++;

                    List<String> newPlan = new ArrayList<>(currentNode.plan());
                    newPlan.add(action.name());

                    frontier.add(new SearchNode(nextState, List.copyOf(newPlan)));
                }
            }
        }

        return Optional.empty();
    }

    static List<GroundAction> instantiateActions(
            List<ActionSchema> schemas,
            List<String> objects
    ) {
        List<GroundAction> groundActions = new ArrayList<>();

        for (ActionSchema schema : schemas) {
            List<List<String>> combinations =
                    generateCombinations(objects, schema.parameters.size());

            for (List<String> combination : combinations) {
                groundActions.add(instantiate(schema, combination));
            }
        }

        return groundActions;
    }

    static List<List<String>> generateCombinations(List<String> objects, int size) {
        List<List<String>> combinations = new ArrayList<>();
        generateCombinationsRecursive(objects, size, new ArrayList<>(), combinations);
        return combinations;
    }

    static void generateCombinationsRecursive(
            List<String> objects,
            int remaining,
            List<String> current,
            List<List<String>> combinations
    ) {
        if (remaining == 0) {
            combinations.add(List.copyOf(current));
            return;
        }

        for (String object : objects) {
            current.add(object);
            generateCombinationsRecursive(objects, remaining - 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }

    static GroundAction instantiate(ActionSchema schema, List<String> values) {
        Map<String, String> bindings = new HashMap<>();

        for (int i = 0; i < schema.parameters.size(); i++) {
            bindings.put(schema.parameters.get(i), values.get(i));
        }

        String name = schema.name + "(" + String.join(",", values) + ")";

        Set<String> preconditions = groundLiterals(schema.preconditions, bindings);
        Set<String> addEffects = groundLiterals(schema.addEffects, bindings);
        Set<String> deleteEffects = groundLiterals(schema.deleteEffects, bindings);

        return new GroundAction(name, preconditions, addEffects, deleteEffects);
    }

    static Set<String> groundLiterals(
            List<List<String>> literals,
            Map<String, String> bindings
    ) {
        Set<String> result = new LinkedHashSet<>();

        for (List<String> literal : literals) {
            String predicate = literal.get(0);
            List<String> arguments = new ArrayList<>();

            for (int i = 1; i < literal.size(); i++) {
                String argument = literal.get(i);
                String bound = bindings.getOrDefault(argument, argument);
                arguments.add(bound);
            }

            if (arguments.isEmpty()) {
                result.add(predicate);
            } else {
                result.add(predicate + "(" + String.join(",", arguments) + ")");
            }
        }

        return result;
    }

    static List<ActionSchema> getActionSchemas(List<Object> domain) {
        List<ActionSchema> schemas = new ArrayList<>();

        for (Object item : domain) {
            if (!(item instanceof List<?> form) || form.isEmpty()) {
                continue;
            }

            if (!":action".equals(form.get(0))) {
                continue;
            }

            ActionSchema schema = new ActionSchema();
            schema.name = (String) form.get(1);

            List<Object> parameters = findForm((List<Object>) form, ":parameters");
            if (parameters != null && parameters.size() > 1) {
                for (Object parameter : parameters.subList(1, parameters.size())) {
                    schema.parameters.add((String) parameter);
                }
            }

            List<Object> precondition = findForm((List<Object>) form, ":precondition");
            if (precondition != null && precondition.size() > 1) {
                collectLiterals(precondition.get(1),
                        schema.preconditions,
                        new ArrayList<>());
            }

            List<Object> effect = findForm(form, ":effect");
            if (effect != null && effect.size() > 1) {
                List<List<String>> positive = new ArrayList<>();
                List<List<String>> negative = new ArrayList<>();

                collectLiterals(effect.get(1), positive, negative);

                schema.addEffects = positive;
                schema.deleteEffects = negative;
            }

            schemas.add(schema);
        }

        return schemas;
    }

    static String getDomainName(List<Object> problem) {
        List<Object> form = findForm(problem, ":domain");

        if (form == null || form.size() < 2) {
            return "";
        }

        return (String) form.get(1);
    }

    static List<String> getObjects(List<Object> problem) {
        List<Object> form = findForm(problem, ":objects");
        List<String> objects = new ArrayList<>();

        if (form == null) {
            return objects;
        }

        for (int i = 1; i < form.size(); i++) {
            if (form.get(i) instanceof String object) {
                objects.add(object);
            }
        }

        return objects;
    }

    static Set<String> getInitialState(List<Object> problem) {
        List<Object> form = findForm(problem, ":init");
        Set<String> initialState = new LinkedHashSet<>();

        if (form == null) {
            return initialState;
        }

        List<List<String>> literals = new ArrayList<>();
        collectLiterals(form, literals, new ArrayList<>());

        for (List<String> literal : literals) {
            initialState.add(formatLiteral(literal));
        }

        return initialState;
    }

    static Set<String> getGoal(List<Object> problem) {
        List<Object> form = findForm(problem, ":goal");
        Set<String> goal = new LinkedHashSet<>();

        if (form == null) {
            return goal;
        }

        List<List<String>> literals = new ArrayList<>();
        collectLiterals(form, literals, new ArrayList<>());

        for (List<String> literal : literals) {
            goal.add(formatLiteral(literal));
        }

        return goal;
    }

    static String formatLiteral(List<String> literal) {
        String predicate = literal.get(0);

        if (literal.size() == 1) {
            return predicate;
        }

        return predicate + "(" + String.join(",", literal.subList(1, literal.size())) + ")";
    }

    static void collectLiterals(
            Object node,
            List<List<String>> positive,
            List<List<String>> negative
    ) {
        if (!(node instanceof List<?> list) || list.isEmpty()) {
            return;
        }

        Object head = list.get(0);

        if ("and".equals(head)) {
            for (int i = 1; i < list.size(); i++) {
                collectLiterals(list.get(i), positive, negative);
            }

            return;
        }

        if ("not".equals(head) && list.size() == 2) {
            if (list.get(1) instanceof List<?> inner && !inner.isEmpty()) {
                List<String> negativeLiteral = new ArrayList<>();

                for (Object element : inner) {
                    if (element instanceof String token) {
                        negativeLiteral.add(token);
                    }
                }

                negative.add(negativeLiteral);
            }

            return;
        }

        List<String> literal = new ArrayList<>();

        for (Object element : list) {
            if (element instanceof String token) {
                literal.add(token);
            }
        }

        if (!literal.isEmpty()) {
            positive.add(literal);
        }
    }

    @SuppressWarnings("unchecked")
    static List<Object> findForm(List<?> forms, String keyword) {
        for (Object item : forms) {
            if (item instanceof List<?> form
                    && !form.isEmpty()
                    && keyword.equals(form.get(0))) {

                return (List<Object>) form;
            }
        }

        return null;
    }

    /**
     * Converte o texto PDDL numa estrutura de listas aninhadas.
     *
     * Exemplo:
     * (on A B) -> ["on", "A", "B"]
     */
    static List<Object> parse(String text) {
        List<String> tokens = tokenize(text);
        int[] position = {0};
        return parseExpression(tokens, position);
    }

    static List<String> tokenize(String text) {
        String normalized = text
                .replaceAll(";[^\\n\\r]*", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);

            if (character == '(' || character == ')') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

                tokens.add(String.valueOf(character));
            } else if (character == ' ') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    static List<Object> parseExpression(List<String> tokens, int[] position) {
        if (!tokens.get(position[0]).equals("(")) {
            throw new IllegalArgumentException("Era esperado '(' na posição " + position[0]);
        }

        position[0]++;
        List<Object> expression = new ArrayList<>();

        while (!tokens.get(position[0]).equals(")")) {
            if (tokens.get(position[0]).equals("(")) {
                expression.add(parseExpression(tokens, position));
            } else {
                expression.add(tokens.get(position[0]));
                position[0]++;
            }
        }

        position[0]++;
        return expression;
    }
}