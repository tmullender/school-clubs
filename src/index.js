const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const log = require('electron-log');
const Store = require('electron-store');
const csv = require('fast-csv');
const fs = require('fs');
const PDFDocument = require('pdfkit');

const PREFERENCE_COUNT = 3;
const store = new Store();

const template = [
  { label: 'File', submenu: [ { role: 'reload', label: 'New' }, 
                              { label: 'Export', click: exportData }, 
                              { label: 'Save'}, 
                              { type: 'separator' },
                              { role: 'Quit'}]},
  { role: 'viewMenu' },
  { role: 'windowMenu' },
]
const menu = Menu.buildFromTemplate(template)
Menu.setApplicationMenu(menu)

String.prototype.capitalize = function() {
  return this.toLowerCase().replace(/(?:^|\s)\S/g, function(a) { return a.toUpperCase(); });
};

function parseTime(timestamp) {
  let parts = timestamp.split(/[\/: ]/);
  let hours = "pm" == parts[6] ? +parts[3] + 12 : parts[3];
  return new Date(+parts[0], +parts[1]-1, +parts[2], hours, +parts[4], +parts[5]);
}

function parseClubs(row, clubs) {
  return Object.keys(row)
    .filter(key => !isNaN(key))
    .reduce((list, key, index) => { 
      let request = row[key];
      if (request.length == 0) {
        list.push(null);
      } else {
        let club = new Club(request, Math.ceil((1 + index)/PREFERENCE_COUNT));
        if (!clubs[club.key]) {
          clubs[club.key] = club;
        }
        list.push(club.key); 
      }
      return list; 
    }, []);
}

class Club {
  constructor(description, term) {
    let parts = description.split(/[()-]/)
    this.name = parts[0].trim();
    this.staff = parts[2].trim();
    this.day = parts[1].trim();
    this.term = term;
    this.maximum = 30;
    this.key = this.name + this.day + this.term;
    this.description = this.name + ' (' + this.day + ')';
    this.allocated = [];
  }
}

class Pupil {
  constructor(row, clubs) {
    this.time = parseTime(row['Time']);
    this.timestamp = this.time.toLocaleString();
    this.name = row['Name'].trim().capitalize();
    this.class = row['Class'];
    this.year = +this.class.charAt(1);
    this.index = this.time.getTime() - this.year * 1000*60*60*24*365; 
    this.count = +row['Count'];
    this.requests = parseClubs(row, clubs);
    this.allocated = [];
  }
}

function hasSpace(club) {
  return club.allocated.length < club.maximum;
}

function isFree(pupil, day, clubs) {
  return pupil.allocated.length < pupil.count && pupil.allocated.filter(clubKey => clubs[clubKey].day == day).length == 0;
}

function allocatePupil(pupil, clubs) {
  let success = pupil.requests.some(clubKey => {
    let club = clubs[clubKey];
    if (club && hasSpace(club) && isFree(pupil, club.day, clubs)){
      log.debug('Allocating ' + club.name + ' for ' + pupil.name);
      pupil.allocated.push(club.key);
      club.allocated.push(pupil.name);
      return true;
    }
    return false;
  });
  log.debug(pupil.name + ' allocated: ' + success);
}

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) { // eslint-disable-line global-require
  app.quit();
}

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

const createWindow = () => {
  // Create the browser window.
  log.info('Creating window');
  mainWindow = new BrowserWindow({
    height: 800,
    icon: `${__dirname}/images/kps.png`,
    minWidth: 600,
    minHeight: 600,
    width: 800,
    webPreferences: {
      preload: `${__dirname}/preload.js`
    }
  });

  // and load the index.html of the app.
  mainWindow.loadURL(`file://${__dirname}/index.html`);

  // Emitted when the window is closed.
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  let pupils = store.get('pupils');
  if (pupils) {
    ipcMain.send('allocation-complete', { pupils: pupils, clubs: store.get('clubs')});
  }
  log.info('Window created');
};

app.on('ready', createWindow);

// Quit when all windows are closed.
app.on('window-all-closed', () => {
  app.quit();
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});

ipcMain.on('start-allocation', (event, input) => {
  log.info('Starting... ' + input);
  let pupils = {};
  let clubs = store.get('clubs', {});
  let classes = {};
  fs.createReadStream(input)
    .pipe(csv.parse({ 
      headers: ['Time', 'Name', 'Class', '1', '2', '3', 'Count'],
      skipLines: 1
    }))
    .on('data', (row) => { 
      log.debug(row);
      let pupil = new Pupil(row, clubs);
      pupils[pupil.name] = pupil;
      if (!classes[pupil.class]) {
        classes[pupil.class] = [];
      }
      if (!classes[pupil.class].includes(pupil.name)) {
        classes[pupil.class].push(pupil.name);
      }
    })
    .on('end', () => {
      log.debug('Sending allocation complete message');
      event.reply('read-complete', { pupils: pupils, clubs: clubs, classes: classes });
    });
});

ipcMain.on('allocate', (event, data) => {
  log.info('Allocating...');
  allocate(data.pupils, data.clubs);
  event.reply('allocation-complete', data);
});

function allocate(pupils, clubs) {
  let order = Object.values(pupils).sort((a, b) => { return a.index - b.index });
  for (let i = 0; i < PREFERENCE_COUNT; i++) {
    log.debug('Allocating iteration: ' + i);
    for (let pupil of order) {
      allocatePupil(pupil, clubs);
    }
    log.debug('Allocated iteration: ' + i);
  }
}

ipcMain.on('save-clubs', (event, data) => {
  store.set('clubs', data);
  log.info(store.path);
})

function exportData() {
  mainWindow.send('export-data');
}

ipcMain.on('export-data', (event, path, data) => {
  let classesDoc = new PDFDocument({ size: 'A4' });
  classesDoc.pipe(fs.createWriteStream(path + '/classes.pdf'));
  Object.keys(data.classes).forEach(className => {
    classesDoc.fontSize(24).text(className);
    classesDoc.fontSize(12);
    let clazz = data.classes[className].flatMap(pupil => [pupil, data.pupils[pupil].allocated.map(club => data.clubs[club].description)]);
    classesDoc.list(clazz, { bulletRadius: 1, columns: 2 });
    classesDoc.addPage();
  });
  classesDoc.end();
  let clubsDoc = new PDFDocument({ size: 'A4' });
  clubsDoc.pipe(fs.createWriteStream(path + '/clubs.pdf'));
  Object.values(data.clubs).forEach(club => {
    clubsDoc.fontSize(24).text(club.description)
    clubsDoc.fontSize(12);
    clubsDoc.list(club.allocated.map(pupil => pupil + " (" + data.pupils[pupil].class + ")"), { bulletRadius: 1, columns: 2 })
    clubsDoc.addPage();
  })
  clubsDoc.end();
  const stream = csv.format({headers: true})
  stream.pipe(fs.createWriteStream(path + '/pupils.csv')).on('end', () => {
    mainWindow.send('export-complete', path);
  });
  Object.values(data.pupils).forEach(pupil => {
    stream.write({'Name': pupil.name, 'Class': pupil.class})
  }) 
})