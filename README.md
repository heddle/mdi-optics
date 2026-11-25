# Light Bench

A laser puzzle built on [MDI](https://github.com/heddle/mdi). Steer the beam
onto every target with mirrors and beam splitters.

![The first level, solved](docs/level-1.png)

## Run it

You need Java 17 or later and Maven.

```bash
mvn compile exec:java
```

## Play

- Click a piece in the palette, then click a grid point to put it there.
- Drag a piece to move it.
- Double-click a piece to turn it one step (22.5°), or select it and drag the
  yellow handle.
- Select a piece and press Delete to take it back.
- Undo and redo with the buttons or Cmd/Ctrl+Z and Cmd/Ctrl+Shift+Z. **Clear**
  removes every piece and can be undone.

A mirror reflects the whole beam. A splitter reflects half and lets half
through. Both work from either side. Walls, targets and the laser stop the beam.
The number under a target is the share of the laser's power it needs.

There are five levels, and you can move between them freely. Each needs every
piece you are given.

## How it is put together

| Package | Contents |
| --- | --- |
| `edu.cnu.mdi.optics.model` | Levels and the ray tracer. No Swing. |
| `edu.cnu.mdi.optics.view` | The board, the pieces and the palette. |

MDI supplies the window, the selecting and dragging of pieces, undo and redo,
and the drawn palette buttons (`DrawnToggleButton`). The items on the board are
the only record of the player's moves: the beam, the lit targets and the pieces
left are worked out from them.

## Add a level

Add `src/main/resources/levels/level-6.json` (and so on, without gaps):

```json
{
  "name": "My level",
  "hint": "Shown beside the board.",
  "laser": { "x": 1, "y": 3, "heading": 90 },
  "targets": [ { "x": 12, "y": 9, "power": 1.0 } ],
  "walls": [ { "x": 6, "y": 6, "heading": 90, "length": 4 } ],
  "mirrors": 1,
  "splitters": 0
}
```

The board runs from (0, 0) at the bottom left to (16, 12). Headings are degrees
clockwise from up. A wall runs at right angles to its heading.

The tests require a known solution for each level, in
`src/test/java/edu/cnu/mdi/optics/model/Solutions.java` (spoilers), and check by
exhaustive search that no level can be solved with fewer pieces than it hands
out. That search grows quickly with the number of pieces.

## Test

```bash
mvn verify
```

A further check opens the game and plays it with mouse events. It needs a
display, and writes screenshots to `target/`:

```bash
mvn test-compile exec:java -Dexec.mainClass=edu.cnu.mdi.optics.view.DesktopSmoke -Dexec.classpathScope=test
```

## License

[MIT](LICENSE)
