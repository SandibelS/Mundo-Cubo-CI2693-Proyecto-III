import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class AlfonsoJose {
    private Pair<Integer, Integer> dimensions;
    private ArrayList<node<Triple<Integer>>> graph;

    private static class node<T>{
        T value;
        int index;
        List<node<T>> successorsList = new ArrayList<node<T>>();

        public node(T value, int index){
            this.value = value;
            this.index = index;
        }

        public void addSuccesor(node<T> succesor){
            this.successorsList.add(succesor); 
        }

    }

    public AlfonsoJose(Pair<Integer, Integer> dimensions, Set<Triple<Integer>> vertexSet){
        this.dimensions = dimensions;
        this.graph =  new ArrayList<node<Triple<Integer>>>();
        int nextIndex = 0;

        for (Triple<Integer> vertex : vertexSet) {
            node<Triple<Integer>> newVertex = new node<Triple<Integer>>(vertex, nextIndex);
            this.graph.add(newVertex);
            nextIndex++;
        }

        // Ok, esto es una forma super ineficiente de conectar los vertices
        // El problema es que la lista de sucesores de un nodo tiene apuntadores
        // a los nodos de forma que se pueda usar esto al momento de calcular 
        // las componentes fuertemente conexas. 
        for (node<Triple<Integer>> v1 : graph) {
            int v1i = v1.value.val1;
            int v1j = v1.value.val2;
            for (node<Triple<Integer>> v2 : graph) {
                int v2i = v2.value.val1;
                int v2j = v2.value.val2;

                if ( (v2i == v1i && (v2j == v1j - 1 || v2j == v1j + 1) )
                     || (v2i == v1i + 1 && (v2j == v1j) )
                     || (v2i == v1i - 1 && (v2j == v1j))) {
                    if(!v1.equals(v2) && v1.value.heigh >= v2.value.heigh){
                        v1.addSuccesor(v2);
                    }
                }
            } 
        } 
    }

    private boolean[][] reach(){
        boolean[][] M = new boolean[graph.size()][graph.size()];

        for (int i = 0; i < graph.size(); i++) {
            reachable(i, i, M);
        }

        return M;
    }

    private void reachable(int v, int w, boolean[][] M){
        M[v][w] = true;
        node<Triple<Integer>> nw = graph.get(w);
        for (node<Triple<Integer>> nz : nw.successorsList) {
            int z = nz.index;
            if (!M[v][z]){
                reachable(v, z, M);
            }
        }
    }

    private int[] stronglyConnectedComponents(){
        boolean[][] M = reach();
        int[] C = new int[graph.size()];

        for (int i = 0; i < C.length; i++) {
            C[i] = -1;
        }

        for (int v = 0; v < C.length; v++) {
            if (C[v] != -1) {
                continue;
            }

            C[v] = v;
            for (int w = 0; w < C.length; w++) {
                if( M[v][w] && M[w][v]){
                    C[w] = v;
                }
            }
        }

        return C;
    }

    private void TopologicalOrderDFS(Integer v, 
                                     boolean[] visited, 
                                     AtomicInteger counter, 
                                     HashMap<Integer, Integer> order, 
                                     HashMap<Integer, Integer>  vertexMap, 
                                     boolean[][] graph) {
        
        Integer nv = vertexMap.get(v);
        Set<Integer> OutwardEdges = new HashSet<Integer>();
        for (Integer keyOther : vertexMap.keySet()) {
            Integer nOther = vertexMap.get(keyOther);
            if (graph[nv][nOther]) {
                OutwardEdges.add(keyOther);
            }
        }

        for (Integer w: OutwardEdges) {
            int j = vertexMap.get(w);
            if (!visited[j]) {
                visited[j] = true;
                TopologicalOrderDFS(w, visited, counter, order, vertexMap, graph);
            }       
        }
        order.put(v, counter.decrementAndGet());
    }

    private HashMap<Integer, Integer> TopologicalOrder(HashMap<Integer, Integer>  vertexMap, 
                                                      boolean[][] graph) {

        int n = vertexMap.size();
        boolean[] visited = new boolean[n];
        AtomicInteger counter = new AtomicInteger(n);
        HashMap<Integer, Integer> order = new HashMap<Integer, Integer>();

        for (Integer v: vertexMap.keySet()) {
            int i = vertexMap.get(v);
            if (!visited[i]) {
                visited[i] = true;
                TopologicalOrderDFS(v, visited, counter, order, vertexMap, graph);
            }
        }

        return order;
    }

    public int calculateNumCubesWater(){
        // Primero calculamos las componentes fuertemente conexas
        int[] C = stronglyConnectedComponents();

        // Ahora creamos el grafo reducido a partir del arreglo C
        HashMap<Integer, Integer> vertices = new HashMap<Integer, Integer>();
        int nextIndex = 0;
        for (int i = 0; i < C.length; i++) {
            if (!vertices.containsKey(C[i])) {
                vertices.put(C[i], nextIndex);
                nextIndex++;
            }
        }

        boolean[][] reducedGraph = new boolean[vertices.size()][vertices.size()];

        // Creamos los arcos del grafo reducido
        for (node<Triple<Integer>> node : graph) {
            // int nodeIndex = graph.indexOf(node);
            int nodeIndex = node.index;
            int nodeRepresentative = C[nodeIndex];
            
            for (node<Triple<Integer>> successor : node.successorsList) {
                
                // int successorIndex = graph.indexOf(successor);
                int successorIndex = successor.index;
                int successorRepresentative = C[successorIndex];

                if (nodeRepresentative != successorRepresentative) {
                    int v = vertices.get(nodeRepresentative);
                    int u = vertices.get(successorRepresentative);
                    reducedGraph[v][u] = true;
                }
            }
        }


        // A partir del grafo reducido, calculamos el orden a ser llenados las componentes.
        // En el hashmap resultante el primer integer es el representante de la componente 
        // y el segundo es su posicion en el orden
        HashMap<Integer, Integer> order =  TopologicalOrder(vertices, reducedGraph);

        // Pasamos el orden obtenido a un arreglo
        int[] arrayOrder = new int[order.size()];
        for (Integer key : order.keySet()) {
            arrayOrder[order.get(key)] = key;
        }

        // Ahora que tenemos el orden, por cada componente que sea un sumidero vamos
        // a buscar el componente predecesor con menor altura y elevar la altura de cada torre
        // teniendo cuidado con las torres en el borde de la ciudad.
        int NumCubesWater = 0;
        for (int r = arrayOrder.length - 1; r > 0 ; r--) {

            // Obtenemos el entero asociado al representante actual
            int i = vertices.get(arrayOrder[r]);
            
            // Primero verificamos si el comoponente tiene sucesores, de ser el 
            // caso no nos interesa este componente ya que estamos buscando sumideros
            //////
            Set<Integer> outwardEdges = new HashSet<Integer>();
            for (Integer other : vertices.keySet()) {
                int indOther = vertices.get(other);
                if (reducedGraph[i][indOther]) {
                    outwardEdges.add(other);
                }
            }

            if (outwardEdges.size() > 0) {
                continue;
            }

            // Ahora vamos a calcular los predecesores y su altura
            Set<Pair<Integer, Integer>> inwardEdges = new HashSet<Pair<Integer, Integer>>();
            for (Integer other : vertices.keySet()) {
                int indOther = vertices.get(other);
                if (reducedGraph[indOther][i]) {
                    int heigh = graph.get(other).value.heigh;
                    Pair<Integer, Integer> newEdge = new Pair<Integer, Integer>(other, heigh);
                    inwardEdges.add(newEdge);
                }
            }

            // Escogemos el que tiene la menor altura
            Pair<Integer, Integer> selectedComponent = new Pair<Integer,Integer>(0, Integer.MAX_VALUE);
            for (Pair<Integer, Integer> edge : inwardEdges) {
                if (edge.val2 < selectedComponent.val2) {
                    selectedComponent = edge;
                }
            }

            // Ahora vamos llenando cada torre de la componente, si alguna esta en el borde no se puede llenar
            for (int j = 0; j < C.length; j++) {
                if (C[j] == arrayOrder[r]) {

                    node<Triple<Integer>> t = graph.get(j);

                    // Continuamos a la siguitente iteracion si la torre esta en el borde
                    if (   t.value.val1 == dimensions.val1 - 1
                        || t.value.val2 == dimensions.val2 - 1
                        || t.value.val1 == 0 
                        || t.value.val2 == 0) {
                        continue;
                    }

                    // Calculamos la cantidad de cubos de agua que esta torre necesita
                    int cubesWater = selectedComponent.val2 - t.value.heigh;
                    NumCubesWater += cubesWater;

                    //Actualizamos a que componente pertenece ahora la torre y su altura
                    C[j] = selectedComponent.val1;
                    t.value.heigh = selectedComponent.val2;
                }
            }
        }
        return NumCubesWater;
    }
    public static void main(String[] args){

        try {
            File input = new File("atlantis.txt");
            Scanner reader = new Scanner(input);

            Set<Triple<Integer>> vertexSet = new HashSet<Triple<Integer>>();
            int numCol = 0;
            int numRow = 0;

            while (reader.hasNextLine()) {
                
                String data = reader.nextLine();
                String[] towers = data.split(" ");
                for (int i = 0; i < towers.length; i++) {
                    Triple<Integer> tower = new Triple<Integer>(numRow, i, Integer.parseInt(towers[i]));
                    vertexSet.add(tower);
                }
                numCol = towers.length;
                numRow++;

            }
            Pair<Integer, Integer> dimensions = new Pair<Integer, Integer>(numRow, numCol);
            reader.close();

            AlfonsoJose solution = new AlfonsoJose(dimensions, vertexSet);
            int numCubesWater = solution.calculateNumCubesWater();
            System.out.println(numCubesWater);

            
        } catch (FileNotFoundException e) {
            System.err.println("Ha ocurrido un error leyendo el archivo atlantis.txt");
        }
    }

}