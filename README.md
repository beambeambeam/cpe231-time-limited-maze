Sawasdekub

to run this program please use ./gradlew run

## Genetic Algorithm Training

Train the genetic algorithm solver on mazes:

```bash
# Train on a specific maze
./gradlew :app:generic -Pmaze=m15_15.txt -Piterations=10

# Train on a random maze
./gradlew :app:generic -Pmaze=random -Piterations=20

# Default: trains on m15_15.txt with 10 iterations
./gradlew :app:generic
```

Trained solutions are cached in `ga_checkpoints/` and will be used automatically by the GUI solver.
