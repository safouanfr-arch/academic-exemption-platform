export enum TypeCorrespondance {
  EQUIVALENCE = 'EQUIVALENCE',
  DISPENSE_PARTIELLE = 'DISPENSE_PARTIELLE',
  DISPENSE_TOTALE = 'DISPENSE_TOTALE'
}

export const TYPE_CORRESPONDANCE_LABELS: Record<TypeCorrespondance, string> = {
  [TypeCorrespondance.EQUIVALENCE]: 'Equivalence',
  [TypeCorrespondance.DISPENSE_PARTIELLE]: 'Dispense partielle',
  [TypeCorrespondance.DISPENSE_TOTALE]: 'Dispense totale'
};
